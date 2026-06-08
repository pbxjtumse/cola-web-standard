package com.xjtu.iron.concurrency.core.execution;

import com.xjtu.iron.concurrency.api.enums.AsyncTaskStatus;
import com.xjtu.iron.concurrency.api.event.TaskExecutionEvent;
import com.xjtu.iron.concurrency.api.exception.AsyncTaskException;
import com.xjtu.iron.concurrency.api.exception.ConcurrencyRejectedException;
import com.xjtu.iron.concurrency.api.execution.AsyncTask;
import com.xjtu.iron.concurrency.api.listener.AsyncUncaughtExceptionHandler;
import com.xjtu.iron.concurrency.api.listener.TaskExecutionListener;
import com.xjtu.iron.concurrency.core.metrics.ConcurrencyMetricsRecorder;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 线程池真正执行的任务命令。
 *
 * <p>这个类是任务执行链路的核心包装：</p>
 * <ul>
 *     <li>记录提交时间、开始时间、结束时间。</li>
 *     <li>区分排队耗时、执行耗时、总耗时。</li>
 *     <li>触发任务监听器。</li>
 *     <li>记录指标。</li>
 *     <li>完成 CompletableFuture。</li>
 *     <li>处理 fire-and-forget 异常。</li>
 * </ul>
 *
 * @param <T> 任务返回值类型
 */
public class TaskCommand<T> implements Runnable, RejectedTaskAware {

    /**
     * 异步任务模型。
     */
    private final AsyncTask<T> task;

    /**
     * 任务结果 Future。fire-and-forget 场景为空。
     */
    private final CompletableFuture<T> future;

    /**
     * 指标记录器。
     */
    private final ConcurrencyMetricsRecorder metricsRecorder;

    /**
     * 任务监听器列表。
     */
    private final List<TaskExecutionListener> listeners;

    /**
     * fire-and-forget 异常处理器。
     */
    private final AsyncUncaughtExceptionHandler uncaughtExceptionHandler;

    /**
     * 提交时间，纳秒。
     */
    private final long submitNanoTime;

    /**
     * 提交时间，毫秒。
     */
    private final long submitTimeMillis;

    /**
     * 开始执行时间，纳秒。
     */
    private volatile long startNanoTime;

    /**
     * 开始执行时间，毫秒。
     */
    private volatile long startTimeMillis;

    /**
     * 当前执行任务的线程。
     */
    private volatile Thread runningThread;

    /**
     * 是否已经通知过超时。
     */
    private final AtomicBoolean timeoutNotified = new AtomicBoolean(false);

    /**
     * 创建任务命令。
     *
     * @param task 异步任务模型
     * @param future 结果 Future。fire-and-forget 场景为空
     * @param metricsRecorder 指标记录器
     * @param listeners 任务监听器列表
     * @param uncaughtExceptionHandler fire-and-forget 异常处理器
     */
    public TaskCommand(
            AsyncTask<T> task,
            CompletableFuture<T> future,
            ConcurrencyMetricsRecorder metricsRecorder,
            List<TaskExecutionListener> listeners,
            AsyncUncaughtExceptionHandler uncaughtExceptionHandler
    ) {
        this.task = task;
        this.future = future;
        this.metricsRecorder = metricsRecorder;
        this.listeners = listeners == null ? Collections.emptyList() : listeners;
        this.uncaughtExceptionHandler = uncaughtExceptionHandler;
        this.submitNanoTime = System.nanoTime();
        this.submitTimeMillis = System.currentTimeMillis();
    }

    /**
     * 创建 fire-and-forget 任务命令。
     *
     * @param task 异步任务模型
     * @param metricsRecorder 指标记录器
     * @param listeners 任务监听器列表
     * @param uncaughtExceptionHandler 异常处理器
     * @return 任务命令
     */
    public static TaskCommand<Void> fireAndForget(
            AsyncTask<Void> task,
            ConcurrencyMetricsRecorder metricsRecorder,
            List<TaskExecutionListener> listeners,
            AsyncUncaughtExceptionHandler uncaughtExceptionHandler
    ) {
        return new TaskCommand<>(task, null, metricsRecorder, listeners, uncaughtExceptionHandler);
    }

    /**
     * 创建带 Future 的任务命令。
     *
     * @param task 异步任务模型
     * @param future 结果 Future
     * @param metricsRecorder 指标记录器
     * @param listeners 任务监听器列表
     * @param uncaughtExceptionHandler 异常处理器
     * @param <T> 返回值类型
     * @return 任务命令
     */
    public static <T> TaskCommand<T> withFuture(
            AsyncTask<T> task,
            CompletableFuture<T> future,
            ConcurrencyMetricsRecorder metricsRecorder,
            List<TaskExecutionListener> listeners,
            AsyncUncaughtExceptionHandler uncaughtExceptionHandler
    ) {
        return new TaskCommand<>(task, future, metricsRecorder, listeners, uncaughtExceptionHandler);
    }

    /**
     * 通知任务已提交。
     */
    public void submitted() {
        metricsRecorder.recordSubmitted(task.getExecutorName(), task.getTaskName(), task.getTags());
        notifySubmitted(buildEvent(AsyncTaskStatus.SUBMITTED, 0L, 0L, 0L, null, "submitted"));
    }

    @Override
    public void run() {
        this.runningThread = Thread.currentThread();
        this.startNanoTime = System.nanoTime();
        this.startTimeMillis = System.currentTimeMillis();

        long queueCostMillis = nanosToMillis(startNanoTime - submitNanoTime);
        metricsRecorder.recordStarted(task.getExecutorName(), task.getTaskName(), queueCostMillis, task.getTags());
        notifyStarted(buildEvent(AsyncTaskStatus.RUNNING, queueCostMillis, 0L, queueCostMillis, null, "started"));

        if (isQueueTimeout(queueCostMillis)) {
            TimeoutException timeoutException = new TimeoutException(
                    "Task queue timeout after " + queueCostMillis + " ms, taskName=" + task.getTaskName()
            );
            onTimeout(timeoutException, queueCostMillis);
            completeExceptionally(timeoutException);
            notifyCompleted(buildEvent(AsyncTaskStatus.TIMEOUT, queueCostMillis, 0L, queueCostMillis, timeoutException, "queue timeout"));
            return;
        }

        try {
            T value = task.getSupplier().get();
            if (timeoutNotified.get() && future != null) {
                return;
            }
            long endNanoTime = System.nanoTime();
            long runCostMillis = nanosToMillis(endNanoTime - startNanoTime);
            long totalCostMillis = nanosToMillis(endNanoTime - submitNanoTime);

            metricsRecorder.recordSuccess(
                    task.getExecutorName(),
                    task.getTaskName(),
                    queueCostMillis,
                    runCostMillis,
                    totalCostMillis,
                    task.getTags()
            );

            TaskExecutionEvent successEvent = buildEvent(
                    AsyncTaskStatus.SUCCESS,
                    queueCostMillis,
                    runCostMillis,
                    totalCostMillis,
                    null,
                    "success"
            );
            notifySuccess(successEvent);

            if (future != null) {
                future.complete(value);
            }

            notifyCompleted(successEvent);
        } catch (Throwable ex) {
            if (timeoutNotified.get() && future != null) {
                return;
            }
            long endNanoTime = System.nanoTime();
            long runCostMillis = nanosToMillis(endNanoTime - startNanoTime);
            long totalCostMillis = nanosToMillis(endNanoTime - submitNanoTime);
            AsyncTaskException taskException = new AsyncTaskException(task.getExecutorName(), task.getTaskName(), ex);

            metricsRecorder.recordFailure(
                    task.getExecutorName(),
                    task.getTaskName(),
                    queueCostMillis,
                    runCostMillis,
                    totalCostMillis,
                    ex,
                    task.getTags()
            );

            TaskExecutionEvent failureEvent = buildEvent(
                    AsyncTaskStatus.FAILED,
                    queueCostMillis,
                    runCostMillis,
                    totalCostMillis,
                    taskException,
                    "failed"
            );
            notifyFailure(failureEvent);

            if (future != null) {
                future.completeExceptionally(taskException);
            } else if (uncaughtExceptionHandler != null) {
                uncaughtExceptionHandler.handleException(failureEvent, taskException);
            }

            notifyCompleted(failureEvent);
        } finally {
            this.runningThread = null;
        }
    }

    @Override
    public void reject(Throwable ex) {
        ConcurrencyRejectedException rejectedException = new ConcurrencyRejectedException(
                task.getExecutorName(),
                task.getTaskName(),
                ex
        );

        metricsRecorder.recordRejected(task.getExecutorName(), task.getTaskName(), task.getTags());
        long totalCostMillis = nanosToMillis(System.nanoTime() - submitNanoTime);
        TaskExecutionEvent event = buildEvent(AsyncTaskStatus.REJECTED, totalCostMillis, 0L, totalCostMillis, rejectedException, "rejected");
        notifyRejected(event);

        if (future != null) {
            future.completeExceptionally(rejectedException);
        }

        notifyCompleted(event);
    }

    /**
     * 通知结果层超时。
     *
     * @param ex 超时异常
     */
    public void timeout(Throwable ex) {
        if (!timeoutNotified.compareAndSet(false, true)) {
            return;
        }

        long elapsedMillis = nanosToMillis(System.nanoTime() - submitNanoTime);
        onTimeout(ex, elapsedMillis);

        if (task.isCancelOnTimeout()) {
            cancelRunningTask();
        }
    }

    /**
     * 通知 fallback 已执行。
     *
     * @param ex 触发 fallback 的异常
     */
    public void fallback(Throwable ex) {
        metricsRecorder.recordFallback(task.getExecutorName(), task.getTaskName(), task.getTags());
        long elapsedMillis = nanosToMillis(System.nanoTime() - submitNanoTime);
        TaskExecutionEvent event = buildEvent(AsyncTaskStatus.FALLBACK, 0L, 0L, elapsedMillis, ex, "fallback");
        notifyFallback(event);
    }

    /**
     * 获取任务模型。
     *
     * @return 任务模型
     */
    public AsyncTask<T> getTask() {
        return task;
    }

    /**
     * 获取任务唯一标识。
     *
     * @return 任务唯一标识
     */
    public String getTaskId() {
        return task.getTaskId();
    }

    /**
     * 获取线程池名称。
     *
     * @return 线程池名称
     */
    public String getExecutorName() {
        return task.getExecutorName();
    }

    /**
     * 获取任务名称。
     *
     * @return 任务名称
     */
    public String getTaskName() {
        return task.getTaskName();
    }

    private boolean isQueueTimeout(long queueCostMillis) {
        Duration queueTimeout = task.getQueueTimeout();
        return queueTimeout != null && queueCostMillis > queueTimeout.toMillis();
    }

    private void onTimeout(Throwable ex, long elapsedMillis) {
        metricsRecorder.recordTimeout(task.getExecutorName(), task.getTaskName(), elapsedMillis, task.getTags());
        TaskExecutionEvent event = buildEvent(AsyncTaskStatus.TIMEOUT, 0L, 0L, elapsedMillis, ex, "timeout");
        notifyTimeout(event);
    }

    private void completeExceptionally(Throwable ex) {
        if (future != null) {
            future.completeExceptionally(ex);
        } else if (uncaughtExceptionHandler != null) {
            uncaughtExceptionHandler.handleException(
                    buildEvent(AsyncTaskStatus.TIMEOUT, 0L, 0L, nanosToMillis(System.nanoTime() - submitNanoTime), ex, "fire-and-forget timeout"),
                    ex
            );
        }
    }

    private void cancelRunningTask() {
        Thread thread = this.runningThread;
        if (thread != null && task.isInterruptOnCancel()) {
            thread.interrupt();
        }
    }

    private TaskExecutionEvent buildEvent(
            AsyncTaskStatus status,
            long queueCostMillis,
            long runCostMillis,
            long totalCostMillis,
            Throwable throwable,
            String message
    ) {
        long endTimeMillis = status == AsyncTaskStatus.SUBMITTED || status == AsyncTaskStatus.RUNNING
                ? 0L
                : System.currentTimeMillis();

        return TaskExecutionEvent.builder()
                .taskId(task.getTaskId())
                .executorName(task.getExecutorName())
                .taskName(task.getTaskName())
                .bizKey(task.getBizKey())
                .description(task.getDescription())
                .tags(task.getTags())
                .status(status)
                .submitTimeMillis(submitTimeMillis)
                .startTimeMillis(startTimeMillis)
                .endTimeMillis(endTimeMillis)
                .queueCostMillis(queueCostMillis)
                .runCostMillis(runCostMillis)
                .totalCostMillis(totalCostMillis)
                .throwable(throwable)
                .message(message)
                .build();
    }

    private long nanosToMillis(long nanos) {
        return Math.max(0L, nanos / 1_000_000L);
    }

    private void notifySubmitted(TaskExecutionEvent event) {
        for (TaskExecutionListener listener : listeners) {
            listener.onSubmitted(event);
        }
    }

    private void notifyStarted(TaskExecutionEvent event) {
        for (TaskExecutionListener listener : listeners) {
            listener.onStarted(event);
        }
    }

    private void notifySuccess(TaskExecutionEvent event) {
        for (TaskExecutionListener listener : listeners) {
            listener.onSuccess(event);
        }
    }

    private void notifyFailure(TaskExecutionEvent event) {
        for (TaskExecutionListener listener : listeners) {
            listener.onFailure(event);
        }
    }

    private void notifyRejected(TaskExecutionEvent event) {
        for (TaskExecutionListener listener : listeners) {
            listener.onRejected(event);
        }
    }

    private void notifyTimeout(TaskExecutionEvent event) {
        for (TaskExecutionListener listener : listeners) {
            listener.onTimeout(event);
        }
    }

    private void notifyFallback(TaskExecutionEvent event) {
        for (TaskExecutionListener listener : listeners) {
            listener.onFallback(event);
        }
    }

    private void notifyCompleted(TaskExecutionEvent event) {
        for (TaskExecutionListener listener : listeners) {
            listener.onCompleted(event);
        }
    }
}
