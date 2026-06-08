package com.xjtu.iron.concurrency.core.execution;

import com.xjtu.iron.concurrency.api.enums.AsyncTaskStatus;
import com.xjtu.iron.concurrency.api.exception.AsyncTaskException;
import com.xjtu.iron.concurrency.api.exception.ConcurrencyRejectedException;
import com.xjtu.iron.concurrency.api.execution.AsyncTask;
import com.xjtu.iron.concurrency.api.execution.TaskExecutionRegistry;
import com.xjtu.iron.concurrency.api.execution.TaskExecutionSnapshot;
import com.xjtu.iron.concurrency.api.listener.AsyncUncaughtExceptionHandler;
import com.xjtu.iron.concurrency.api.event.TaskExecutionEvent;
import com.xjtu.iron.concurrency.api.listener.TaskExecutionListener;
import com.xjtu.iron.concurrency.core.metrics.ConcurrencyMetricsRecorder;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * 线程池真正执行的任务命令。
 *
 * <p>负责执行 Supplier，记录排队耗时、执行耗时、总耗时，并推动 Future 完成。</p>
 */
public class TaskCommand<T> implements Runnable, RejectedTaskAware {

    private final AsyncTask<T> task;
    private final Supplier<T> supplier;
    private final CompletableFuture<T> future;
    private final ConcurrencyMetricsRecorder metricsRecorder;
    private final List<TaskExecutionListener> listeners;
    private final AsyncUncaughtExceptionHandler uncaughtExceptionHandler;
    private final TaskExecutionRegistry taskExecutionRegistry;
    private final boolean fireAndForget;
    private final long submitTimeMillis;
    private volatile Thread runningThread;

    public TaskCommand(
            AsyncTask<T> task,
            Supplier<T> supplier,
            CompletableFuture<T> future,
            ConcurrencyMetricsRecorder metricsRecorder,
            List<TaskExecutionListener> listeners,
            AsyncUncaughtExceptionHandler uncaughtExceptionHandler,
            TaskExecutionRegistry taskExecutionRegistry,
            boolean fireAndForget
    ) {
        this.task = task;
        this.supplier = supplier;
        this.future = future;
        this.metricsRecorder = metricsRecorder;
        this.listeners = listeners;
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

    /** 尝试取消底层执行线程。 */
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

    private void notifyStarted(TaskExecutionEvent event) { listeners.forEach(listener -> listener.onStarted(event.copy())); }
    private void notifySuccess(TaskExecutionEvent event) { listeners.forEach(listener -> listener.onSuccess(event.copy())); }
    private void notifyFailure(TaskExecutionEvent event) { listeners.forEach(listener -> listener.onFailure(event.copy())); }
    private void notifyRejected(TaskExecutionEvent event) { listeners.forEach(listener -> listener.onRejected(event.copy())); }
    private void notifyTimeout(TaskExecutionEvent event) { listeners.forEach(listener -> listener.onTimeout(event.copy())); }
    private void notifyCompleted(TaskExecutionEvent event) { listeners.forEach(listener -> listener.onCompleted(event.copy())); }
}
