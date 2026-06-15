package com.xjtu.iron.concurrency.core.task;

import com.xjtu.iron.concurrency.api.enums.error.AsyncErrorCategory;
import com.xjtu.iron.concurrency.api.enums.error.AsyncErrorReason;
import com.xjtu.iron.concurrency.api.enums.error.AsyncErrorStage;
import com.xjtu.iron.concurrency.api.enums.error.AsyncRecoveryAction;
import com.xjtu.iron.concurrency.api.enums.task.AsyncTaskStatus;
import com.xjtu.iron.concurrency.api.error.ApplicationErrorInfo;
import com.xjtu.iron.concurrency.api.error.AsyncError;
import com.xjtu.iron.concurrency.api.error.AsyncErrorClassification;
import com.xjtu.iron.concurrency.api.error.AsyncErrorClassifier;
import com.xjtu.iron.concurrency.api.error.ExceptionInfo;
import com.xjtu.iron.concurrency.api.error.RecoveryHint;
import com.xjtu.iron.concurrency.api.exception.AsyncTaskException;
import com.xjtu.iron.concurrency.api.exception.ConcurrencyRejectedException;
import com.xjtu.iron.concurrency.api.execution.registry.TaskExecutionRegistry;
import com.xjtu.iron.concurrency.api.execution.registry.TaskExecutionSnapshot;
import com.xjtu.iron.concurrency.api.execution.task.AsyncTask;
import com.xjtu.iron.concurrency.api.event.TaskExecutionEvent;
import com.xjtu.iron.concurrency.api.listener.AsyncUncaughtExceptionHandler;
import com.xjtu.iron.concurrency.api.listener.TaskExecutionListener;
import com.xjtu.iron.concurrency.core.metrics.ConcurrencyMetricsRecorder;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * 线程池真正执行的任务命令。
 *
 * <p>
 * TaskCommand 是最终提交给 ThreadPoolExecutor 的 Runnable。
 * 它负责执行用户任务，并把任务状态同步到 CompletableFuture、指标、监听器和任务状态注册表。
 * </p>
 *
 * @param <T> 任务返回值类型
 */
public class TaskCommand<T> implements Runnable, RejectedTaskAware {

    /** 任务模型，包含 executorName、taskName、taskId、bizKey、timeout、fallback 等元数据。 */
    private final AsyncTask<T> task;

    /** 实际执行业务逻辑的 Supplier。Runnable 类型任务会被适配成返回 null 的 Supplier。 */
    private final Supplier<T> supplier;

    /** 任务结果 Future。run/supply/submit 会使用；execute 类型任务虽然也创建 Future，但调用方不感知。 */
    private final CompletableFuture<T> future;

    /** 指标记录器，用于记录提交、开始、成功、失败、拒绝、超时、取消、fallback、完成等指标。 */
    private final ConcurrencyMetricsRecorder metricsRecorder;

    /** 任务监听器。这里建议传入 CompositeTaskExecutionListener，TaskCommand 不关心有多少真实监听器。 */
    private final TaskExecutionListener taskExecutionListener;

    /** fire-and-forget 异常处理器。execute 方法没有 Future 返回值，异常通过该处理器兜底。 */
    private final AsyncUncaughtExceptionHandler uncaughtExceptionHandler;

    /** 任务状态注册表，用于保存最近任务状态，支持通过 taskId 查询。 */
    private final TaskExecutionRegistry taskExecutionRegistry;

    /** 异步错误分类器，用于把 Throwable 转换为结构化 AsyncError。 */
    private final AsyncErrorClassifier asyncErrorClassifier;



    public TaskCommand(
            AsyncTask<T> task,
            Supplier<T> supplier,
            CompletableFuture<T> future,
            ConcurrencyMetricsRecorder metricsRecorder,
            TaskExecutionListener taskExecutionListener,
            AsyncUncaughtExceptionHandler uncaughtExceptionHandler,
            TaskExecutionRegistry taskExecutionRegistry,
            AsyncErrorClassifier asyncErrorClassifier,
            boolean fireAndForget
    ) {
        this.task = task;
        this.supplier = supplier;
        this.future = future;
        this.metricsRecorder = metricsRecorder;
        this.taskExecutionListener = taskExecutionListener;
        this.uncaughtExceptionHandler = uncaughtExceptionHandler;
        this.taskExecutionRegistry = taskExecutionRegistry;
        this.asyncErrorClassifier = asyncErrorClassifier;
        this.fireAndForget = fireAndForget;
        this.submitTimeMillis = System.currentTimeMillis();
    }

    @Override
    public void run() {
        this.runningThread = Thread.currentThread();
        this.startTimeMillis = System.currentTimeMillis();

        if (isQueueTimeout()) {
            TimeoutException timeout = new TimeoutException("Task queue timeout, queueCostMillis=" + calculateQueueCostMillis());
            completeTimeout(timeout, AsyncErrorStage.QUEUE);
            this.runningThread = null;
            return;
        }

        TaskExecutionEvent started = buildEvent(AsyncTaskStatus.RUNNING, AsyncError.none());
        publishStarted(started);

        try {
            T value = supplier.get();
            completeSuccess(value);
        } catch (Throwable ex) {
            completeFailure(ex);
        } finally {
            this.runningThread = null;
        }
    }

    @Override
    public void reject(Throwable throwable) {
        if (!completed.compareAndSet(false, true)) {
            return;
        }

        this.endTimeMillis = System.currentTimeMillis();
        AsyncError error = rejectedError(throwable);
        ConcurrencyRejectedException rejectedException = new ConcurrencyRejectedException(
                task.getExecutorName(),
                task.getTaskName(),
                error,
                throwable instanceof RejectedExecutionException ? throwable : new RejectedExecutionException(throwable)
        );

        TaskExecutionEvent event = buildEvent(AsyncTaskStatus.REJECTED, error);
        if (!fireAndForget && future != null) {
            future.completeExceptionally(rejectedException);
        }

        metricsRecorder.recordRejected(event.copy());
        taskExecutionRegistry.update(TaskExecutionSnapshot.from(event));
        taskExecutionListener.onRejected(event.copy());
        publishCompleted(event);
    }

    /**
     * 标记结果层超时或排队超时。
     *
     * @param throwable 超时异常
     * @param stage 超时发生阶段：QUEUE 表示排队超时；WAIT_RESULT 表示结果层超时
     */
    public void completeTimeout(Throwable throwable, AsyncErrorStage stage) {
        if (!completed.compareAndSet(false, true)) {
            return;
        }

        this.endTimeMillis = System.currentTimeMillis();
        AsyncError error = timeoutError(throwable, stage);
        AsyncTaskException timeoutException = new AsyncTaskException(
                task.getExecutorName(),
                task.getTaskName(),
                task.getTaskId(),
                error,
                throwable
        );

        TaskExecutionEvent event = buildEvent(AsyncTaskStatus.TIMEOUT, error);
        if (!fireAndForget && future != null) {
            future.completeExceptionally(timeoutException);
        }

        metricsRecorder.recordTimeout(event.copy());
        taskExecutionRegistry.update(TaskExecutionSnapshot.from(event));
        taskExecutionListener.onTimeout(event.copy());
        publishCompleted(event);
    }

    /**
     * 标记任务取消。
     */
    public void completeCancelled(Throwable throwable, boolean mayInterruptIfRunning) {
        if (!completed.compareAndSet(false, true)) {
            return;
        }

        this.endTimeMillis = System.currentTimeMillis();
        AsyncError error = cancelledError(throwable);
        TaskExecutionEvent event = buildEvent(AsyncTaskStatus.CANCELLED, error);

        if (!fireAndForget && future != null) {
            future.cancel(mayInterruptIfRunning);
        }
        interruptRunningIfNecessary(mayInterruptIfRunning);

        metricsRecorder.recordCancelled(event.copy());
        taskExecutionRegistry.update(TaskExecutionSnapshot.from(event));
        taskExecutionListener.onCancelled(event.copy());
        publishCompleted(event);
    }

    /**
     * 超时后仅尝试中断底层线程，不改变已记录的 TIMEOUT 状态。
     */
    public void interruptRunningIfNecessary(boolean mayInterruptIfRunning) {
        if (mayInterruptIfRunning && runningThread != null) {
            runningThread.interrupt();
        }
    }

    private void completeSuccess(T value) {
        if (!completed.compareAndSet(false, true)) {
            return;
        }

        this.endTimeMillis = System.currentTimeMillis();
        TaskExecutionEvent event = buildEvent(AsyncTaskStatus.SUCCESS, AsyncError.none());

        if (!fireAndForget && future != null) {
            future.complete(value);
        }

        metricsRecorder.recordSuccess(event.copy());
        taskExecutionRegistry.update(TaskExecutionSnapshot.from(event));
        taskExecutionListener.onSuccess(event.copy());
        publishCompleted(event);
    }

    private void completeFailure(Throwable throwable) {
        if (!completed.compareAndSet(false, true)) {
            return;
        }

        this.endTimeMillis = System.currentTimeMillis();
        AsyncError error = asyncErrorClassifier.classify(task, throwable, AsyncErrorStage.RUN);
        AsyncTaskException taskException = new AsyncTaskException(
                task.getExecutorName(),
                task.getTaskName(),
                task.getTaskId(),
                error,
                throwable
        );

        TaskExecutionEvent event = buildEvent(AsyncTaskStatus.FAILED, error);
        if (fireAndForget) {
            uncaughtExceptionHandler.handleException(event.copy(), taskException);
        } else if (future != null) {
            future.completeExceptionally(taskException);
        }

        metricsRecorder.recordFailure(event.copy());
        taskExecutionRegistry.update(TaskExecutionSnapshot.from(event));
        taskExecutionListener.onFailure(event.copy());
        publishCompleted(event);
    }

    private void publishStarted(TaskExecutionEvent event) {
        metricsRecorder.recordStarted(event.copy());
        taskExecutionRegistry.update(TaskExecutionSnapshot.from(event));
        taskExecutionListener.onStarted(event.copy());
    }

    private void publishCompleted(TaskExecutionEvent event) {
        metricsRecorder.recordCompleted(event.copy());
        taskExecutionListener.onCompleted(event.copy());
    }

    private TaskExecutionEvent buildEvent(AsyncTaskStatus status, AsyncError error) {
        TaskExecutionEvent event = new TaskExecutionEvent();
        event.setTaskId(task.getTaskId());
        event.setExecutorName(task.getExecutorName());
        event.setTaskName(task.getTaskName());
        event.setBizKey(task.getBizKey());
        event.setDescription(task.getDescription());
        event.setTags(task.getTags());
        event.setStatus(status);
        event.setError(error == null ? AsyncError.none() : error);
        event.setSubmitTimeMillis(submitTimeMillis);
        event.setStartTimeMillis(startTimeMillis);
        event.setEndTimeMillis(endTimeMillis);
        event.setQueueCostMillis(calculateQueueCostMillis());
        event.setRunCostMillis(calculateRunCostMillis());
        event.setTotalCostMillis(calculateTotalCostMillis());
        event.setFireAndForget(fireAndForget);
        return event;
    }

    private boolean isQueueTimeout() {
        if (task.getQueueTimeout() == null || task.getQueueTimeout().isZero() || task.getQueueTimeout().isNegative()) {
            return false;
        }
        return calculateQueueCostMillis() > task.getQueueTimeout().toMillis();
    }

    private long calculateQueueCostMillis() {
        if (startTimeMillis > 0) {
            return Math.max(0, startTimeMillis - submitTimeMillis);
        }
        if (endTimeMillis > 0) {
            return Math.max(0, endTimeMillis - submitTimeMillis);
        }
        return Math.max(0, System.currentTimeMillis() - submitTimeMillis);
    }

    private long calculateRunCostMillis() {
        if (startTimeMillis <= 0 || endTimeMillis <= 0) {
            return 0;
        }
        return Math.max(0, endTimeMillis - startTimeMillis);
    }

    private long calculateTotalCostMillis() {
        if (endTimeMillis <= 0) {
            return 0;
        }
        return Math.max(0, endTimeMillis - submitTimeMillis);
    }

    private AsyncError rejectedError(Throwable throwable) {
        return AsyncError.builder()
                .classification(AsyncErrorClassification.of(
                        AsyncErrorCategory.RESOURCE,
                        AsyncErrorReason.REJECTED,
                        AsyncErrorStage.SUBMIT
                ))
                .application(ApplicationErrorInfo.none())
                .exception(ExceptionInfo.from(throwable))
                .recovery(RecoveryHint.of(AsyncRecoveryAction.DELAY_RETRY, true, false, true))
                .build();
    }

    private AsyncError timeoutError(Throwable throwable, AsyncErrorStage stage) {
        AsyncErrorReason reason = stage == AsyncErrorStage.QUEUE ? AsyncErrorReason.QUEUE_TIMEOUT : AsyncErrorReason.TIMEOUT;
        return AsyncError.builder()
                .classification(AsyncErrorClassification.of(
                        stage == AsyncErrorStage.QUEUE ? AsyncErrorCategory.RESOURCE : AsyncErrorCategory.DEPENDENCY,
                        reason,
                        stage == null ? AsyncErrorStage.WAIT_RESULT : stage
                ))
                .application(ApplicationErrorInfo.none())
                .exception(ExceptionInfo.from(throwable))
                .recovery(RecoveryHint.of(
                        stage == AsyncErrorStage.QUEUE ? AsyncRecoveryAction.DELAY_RETRY : AsyncRecoveryAction.FAST_RETRY,
                        true,
                        false,
                        stage == AsyncErrorStage.QUEUE
                ))
                .build();
    }

    private AsyncError cancelledError(Throwable throwable) {
        return AsyncError.builder()
                .classification(AsyncErrorClassification.of(
                        AsyncErrorCategory.COMPONENT,
                        AsyncErrorReason.CANCELLED,
                        AsyncErrorStage.CANCEL
                ))
                .application(ApplicationErrorInfo.none())
                .exception(ExceptionInfo.from(throwable))
                .recovery(RecoveryHint.none())
                .build();
    }
}
