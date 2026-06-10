package com.xjtu.iron.concurrency.core.task;

import com.xjtu.iron.concurrency.api.enums.task.AsyncTaskStatus;
import com.xjtu.iron.concurrency.api.exception.AsyncTaskException;
import com.xjtu.iron.concurrency.api.exception.ConcurrencyRejectedException;
import com.xjtu.iron.concurrency.api.execution.task.AsyncTask;
import com.xjtu.iron.concurrency.api.execution.registry.TaskExecutionRegistry;
import com.xjtu.iron.concurrency.api.execution.registry.TaskExecutionSnapshot;
import com.xjtu.iron.concurrency.api.listener.AsyncUncaughtExceptionHandler;
import com.xjtu.iron.concurrency.api.event.TaskExecutionEvent;
import com.xjtu.iron.concurrency.api.listener.TaskExecutionListener;
import com.xjtu.iron.concurrency.core.metrics.ConcurrencyMetricsRecorder;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * 线程池真正执行的任务命令。
 *
 * <p>负责执行 Supplier，记录排队耗时、执行耗时、总耗时，并推动 Future 完成。</p>
 */
public class TaskCommand<T> implements Runnable, RejectedTaskAware {
    /**
     * 任务模型，包含 executorName、taskName、taskId、bizKey、timeout、fallback 等元数据。
     */
    private final AsyncTask<T> task;
    /**
     * 实际执行业务逻辑的 Supplier。
     *
     * <p>
     * 对 Runnable 类型任务，会被适配成返回 null 的 Supplier。
     * </p>
     */
    private final Supplier<T> supplier;
    /**
     * 任务结果 Future。
     *
     * <p>
     * run/supply/submit 会传入该对象，用于向调用方返回执行结果。
     * execute 是 fire-and-forget 任务，可以没有 future。
     * </p>
     */
    private final CompletableFuture<T> future;
    /**
     * 指标记录器。
     *
     * <p>
     * 用于记录 submitted、started、success、failure、rejected、timeout、fallback、completed 等指标。
     * </p>
     */
    private final ConcurrencyMetricsRecorder metricsRecorder;
    /**
     * 任务监听器。
     *
     * <p>
     * 用于通知业务扩展点，例如审计日志、业务事件、告警等。
     * </p>
     */
    private final List<TaskExecutionListener> taskExecutionListeners;
    /**
     * fire-and-forget 异常处理器。
     *
     * <p>
     * execute 方法没有 CompletableFuture 返回值，因此异常无法交给调用方处理。
     * 该处理器用于统一处理 execute 类型任务的未捕获异常。
     * </p>
     */
    private final AsyncUncaughtExceptionHandler uncaughtExceptionHandler;
    /**
     * 任务状态注册表。
     *
     * <p>
     * 用于保存任务最近状态，后续可通过 taskId 查询任务执行情况。
     * </p>
     */
    private final TaskExecutionRegistry taskExecutionRegistry;
    /**
     * 是否 fire-and-forget 任务。
     *
     * <p>
     * true 表示来自 execute 方法，调用方不关心返回值。
     * false 表示来自 run/supply/submit，调用方通过 CompletableFuture 感知结果。
     * </p>
     */
    private final boolean fireAndForget;
    /**
     * 任务提交时间。
     *
     * <p>
     * 用于计算 queueCostMillis 和 totalCostMillis。
     * </p>
     */
    private final long submitTimeMillis;
    /**
     * 当前正在执行该任务的线程。
     *
     * <p>
     * 用于 cancelOnTimeout + interruptOnCancel 场景下尝试中断正在运行的任务。
     * </p>
     */
    private volatile Thread runningThread;
    /**
     * 任务开始执行时间。
     *
     * <p>
     * 在线程池工作线程真正开始执行 run 方法时赋值。
     * </p>
     */
    private volatile long startTimeMillis;
    /**
     * 任务结束时间。
     *
     * <p>
     * 任务成功、失败、拒绝、取消、超时时可能会更新。
     * </p>
     */
    private volatile long endTimeMillis;
    /**
     * 任务是否已经进入终态。
     *
     * <p>
     * 用于避免成功、失败、超时、拒绝等事件重复完成。
     * </p>
     */
    private final AtomicBoolean completed = new AtomicBoolean(false);

    public TaskCommand(
            AsyncTask<T> task,
            Supplier<T> supplier,
            CompletableFuture<T> future,
            ConcurrencyMetricsRecorder metricsRecorder,
            List<TaskExecutionListener> taskExecutionListeners,
            AsyncUncaughtExceptionHandler uncaughtExceptionHandler,
            TaskExecutionRegistry taskExecutionRegistry,
            boolean fireAndForget
    ) {
        this.task = task;
        this.supplier = supplier;
        this.future = future;
        this.metricsRecorder = metricsRecorder;
        this.taskExecutionListeners = taskExecutionListeners;
        this.uncaughtExceptionHandler = uncaughtExceptionHandler;
        this.taskExecutionRegistry = taskExecutionRegistry;
        this.fireAndForget = fireAndForget;
        this.submitTimeMillis = System.currentTimeMillis();
    }

    @Override
    public void run() {
        this.runningThread = Thread.currentThread();
        long startTimeMillis = System.currentTimeMillis();

        if (task.getQueueTimeout() != null && !task.getQueueTimeout().isZero()) {
            long queueCost = startTimeMillis - submitTimeMillis;
            if (queueCost > task.getQueueTimeout().toMillis()) {
                AsyncTaskException timeoutException = new AsyncTaskException(
                        task.getExecutorName(),
                        task.getTaskName(),
                        new java.util.concurrent.TimeoutException("Task queue timeout, queueCostMillis=" + queueCost)
                );
                TaskExecutionEvent event = baseEvent(AsyncTaskStatus.TIMEOUT);
                event.setStartTimeMillis(startTimeMillis);
                event.setEndTimeMillis(startTimeMillis);
                event.setQueueCostMillis(queueCost);
                event.setRunCostMillis(0);
                event.setTotalCostMillis(queueCost);
                event.setThrowable(timeoutException);
                metricsRecorder.recordTimeout(event.copy());
                notifyTimeout(event);
                completeExceptionally(timeoutException, event);
                return;
            }
        }

        TaskExecutionEvent started = baseEvent(AsyncTaskStatus.RUNNING);
        started.setStartTimeMillis(startTimeMillis);
        started.setQueueCostMillis(startTimeMillis - submitTimeMillis);
        metricsRecorder.recordStarted(started.copy());
        notifyStarted(started);
        taskExecutionRegistry.update(toSnapshot(started));

        try {
            T value = supplier.get();
            long endTimeMillis = System.currentTimeMillis();

            TaskExecutionEvent success = baseEvent(AsyncTaskStatus.SUCCESS);
            success.setStartTimeMillis(startTimeMillis);
            success.setEndTimeMillis(endTimeMillis);
            success.setQueueCostMillis(startTimeMillis - submitTimeMillis);
            success.setRunCostMillis(endTimeMillis - startTimeMillis);
            success.setTotalCostMillis(endTimeMillis - submitTimeMillis);

            future.complete(value);
            metricsRecorder.recordSuccess(success.copy());
            notifySuccess(success);
            complete(success);
        } catch (Throwable ex) {
            long endTimeMillis = System.currentTimeMillis();
            AsyncTaskException taskException = new AsyncTaskException(task.getExecutorName(), task.getTaskName(), ex);

            TaskExecutionEvent failure = baseEvent(AsyncTaskStatus.FAILED);
            failure.setStartTimeMillis(startTimeMillis);
            failure.setEndTimeMillis(endTimeMillis);
            failure.setQueueCostMillis(startTimeMillis - submitTimeMillis);
            failure.setRunCostMillis(endTimeMillis - startTimeMillis);
            failure.setTotalCostMillis(endTimeMillis - submitTimeMillis);
            failure.setThrowable(taskException);

            if (fireAndForget) {
                uncaughtExceptionHandler.handleException(failure.copy(), taskException);
            } else {
                future.completeExceptionally(taskException);
            }

            metricsRecorder.recordFailure(failure.copy());
            notifyFailure(failure);
            complete(failure);
        } finally {
            this.runningThread = null;
        }
    }

    @Override
    public void reject(Throwable throwable) {
        ConcurrencyRejectedException rejectedException = new ConcurrencyRejectedException(
                task.getExecutorName(),
                task.getTaskName(),
                throwable
        );
        TaskExecutionEvent event = baseEvent(AsyncTaskStatus.REJECTED);
        long now = System.currentTimeMillis();
        event.setEndTimeMillis(now);
        event.setQueueCostMillis(now - submitTimeMillis);
        event.setTotalCostMillis(now - submitTimeMillis);
        event.setThrowable(rejectedException);

        if (!fireAndForget) {
            future.completeExceptionally(rejectedException);
        }
        metricsRecorder.recordRejected(event.copy());
        notifyRejected(event);
        complete(event);
    }

    /**
     * 尝试取消底层执行线程。
     */
    public void cancelRunning(boolean mayInterruptIfRunning) {
        future.cancel(mayInterruptIfRunning);
        if (mayInterruptIfRunning && runningThread != null) {
            runningThread.interrupt();
        }
    }

    private void completeExceptionally(Throwable throwable, TaskExecutionEvent event) {
        if (!fireAndForget) {
            future.completeExceptionally(throwable);
        }
        complete(event);
    }

    private void complete(TaskExecutionEvent event) {
        metricsRecorder.recordCompleted(event.copy());
        notifyCompleted(event);
        taskExecutionRegistry.update(toSnapshot(event));
    }

    private TaskExecutionEvent baseEvent(AsyncTaskStatus status) {
        TaskExecutionEvent event = new TaskExecutionEvent();
        event.setTaskId(task.getTaskId());
        event.setExecutorName(task.getExecutorName());
        event.setTaskName(task.getTaskName());
        event.setBizKey(task.getBizKey());
        event.setDescription(task.getDescription());
        event.setTags(task.getTags());
        event.setStatus(status);
        event.setSubmitTimeMillis(submitTimeMillis);
        event.setFireAndForget(fireAndForget);
        return event;
    }

    private TaskExecutionSnapshot toSnapshot(TaskExecutionEvent event) {
        TaskExecutionSnapshot snapshot = new TaskExecutionSnapshot();
        snapshot.setTaskId(event.getTaskId());
        snapshot.setExecutorName(event.getExecutorName());
        snapshot.setTaskName(event.getTaskName());
        snapshot.setBizKey(event.getBizKey());
        snapshot.setDescription(event.getDescription());
        snapshot.setTags(event.getTags());
        snapshot.setStatus(event.getStatus());
        snapshot.setSubmitTimeMillis(event.getSubmitTimeMillis());
        snapshot.setStartTimeMillis(event.getStartTimeMillis());
        snapshot.setEndTimeMillis(event.getEndTimeMillis());
        snapshot.setQueueCostMillis(event.getQueueCostMillis());
        snapshot.setRunCostMillis(event.getRunCostMillis());
        snapshot.setTotalCostMillis(event.getTotalCostMillis());
        if (event.getThrowable() != null) {
            snapshot.setErrorType(event.getThrowable().getClass().getName());
            snapshot.setErrorMessage(event.getThrowable().getMessage());
        }
        return snapshot;
    }

    private void notifyStarted(TaskExecutionEvent event) {
        taskExecutionListeners.forEach(listener -> listener.onStarted(event.copy()));
    }

    private void notifySuccess(TaskExecutionEvent event) {
        taskExecutionListeners.forEach(listener -> listener.onSuccess(event.copy()));
    }

    private void notifyFailure(TaskExecutionEvent event) {
        taskExecutionListeners.forEach(listener -> listener.onFailure(event.copy()));
    }

    private void notifyRejected(TaskExecutionEvent event) {
        taskExecutionListeners.forEach(listener -> listener.onRejected(event.copy()));
    }

    private void notifyTimeout(TaskExecutionEvent event) {
        taskExecutionListeners.forEach(listener -> listener.onTimeout(event.copy()));
    }

    private void notifyCompleted(TaskExecutionEvent event) {
        taskExecutionListeners.forEach(listener -> listener.onCompleted(event.copy()));
    }
}
