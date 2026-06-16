package com.xjtu.iron.concurrency.core.task;

import com.xjtu.iron.concurrency.api.enums.error.AsyncErrorStage;
import com.xjtu.iron.concurrency.api.enums.task.AsyncTaskStatus;
import com.xjtu.iron.concurrency.api.error.AsyncError;
import com.xjtu.iron.concurrency.api.error.AsyncErrorClassifier;
import com.xjtu.iron.concurrency.api.event.TaskExecutionEvent;
import com.xjtu.iron.concurrency.api.exception.AsyncTaskException;
import com.xjtu.iron.concurrency.api.exception.ConcurrencyRejectedException;
import com.xjtu.iron.concurrency.api.listener.AsyncUncaughtExceptionHandler;
import com.xjtu.iron.concurrency.core.lifecycle.TaskLifecyclePublisher;

import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * 线程池真正执行的任务命令。
 *
 * <p>
 * TaskCommand 只负责原始任务：提交、开始、成功、失败、拒绝、超时和取消。
 * timeout/fallback 组成的结果恢复管道由 TaskResultPipeline 负责。
 * </p>
 *
 * @param <T> 任务返回值类型
 */
public final class TaskCommand<T> implements Runnable, RejectedTaskAware {

    /**
     * 本次任务执行上下文，是任务定义、装饰后执行逻辑、Future 和运行时状态的唯一数据来源。
     */
    private final TaskExecutionContext<T> context;

    /**
     * 生命周期发布器，统一分发指标、注册表和监听器。
     */
    private final TaskLifecyclePublisher lifecyclePublisher;

    /**
     * 组合后的错误分类器。
     */
    private final AsyncErrorClassifier errorClassifier;

    /**
     * fire-and-forget 任务异常处理器。
     */
    private final AsyncUncaughtExceptionHandler uncaughtExceptionHandler;

    public TaskCommand(
            TaskExecutionContext<T> context,
            TaskLifecyclePublisher lifecyclePublisher,
            AsyncErrorClassifier errorClassifier,
            AsyncUncaughtExceptionHandler uncaughtExceptionHandler
    ) {
        this.context = Objects.requireNonNull(context, "context must not be null");
        this.lifecyclePublisher = Objects.requireNonNull(lifecyclePublisher, "lifecyclePublisher must not be null");
        this.errorClassifier = Objects.requireNonNull(errorClassifier, "errorClassifier must not be null");
        this.uncaughtExceptionHandler = Objects.requireNonNull(
                uncaughtExceptionHandler,
                "uncaughtExceptionHandler must not be null"
        );
    }

    /**
     * 发布任务已提交事件。
     */
    public void submitted() {
        context.getRuntime().markSubmitted();
        lifecyclePublisher.publish(context.event(
                AsyncTaskStatus.SUBMITTED,
                AsyncError.none(),
                "Task submitted"
        ));
    }

    @Override
    public void run() {
        TaskExecutionRuntime runtime = context.getRuntime();

        /*
         * 结果层超时可能在线程真正从队列取出任务前已经确定原始结果。
         * 此时不应该继续执行用户 operation。
         */
        if (runtime.isBaseOutcomeResolved()) {
            return;
        }

        if (runtime.isQueueTimeout(context.getTask().getQueueTimeout())) {
            TimeoutException timeout = new TimeoutException("Task queue timeout");
            completeTimeout(timeout, AsyncErrorStage.QUEUE);
            return;
        }

        /*
         * 工作线程开始运行与结果层超时可能并发发生。tryMarkRunning 与
         * tryResolveBaseOutcome 使用同一状态锁，确保 TIMEOUT 不会再被 RUNNING 覆盖。
         */
        if (!runtime.tryMarkRunning()) {
            return;
        }

        lifecyclePublisher.publish(context.event(
                AsyncTaskStatus.RUNNING,
                AsyncError.none(),
                "Task started"
        ));

        try {
            T value = context.getExecutable().get();
            completeSuccess(value);
        } catch (Throwable throwable) {
            completeFailure(throwable);
        } finally {
            runtime.clearRunningThread();
        }
    }

    /**
     * 在线程池拒绝任务时完成原始任务。
     *
     * @param throwable 拒绝异常
     */
    @Override
    public void reject(Throwable throwable) {
        TaskExecutionRuntime runtime = context.getRuntime();
        if (!runtime.tryResolveBaseOutcome(AsyncTaskStatus.REJECTED)) {
            return;
        }

        Throwable rejectedCause = throwable instanceof RejectedExecutionException
                ? throwable
                : new RejectedExecutionException("Task rejected", throwable);
        AsyncError error = errorClassifier.classify(
                context.getTask(),
                rejectedCause,
                AsyncErrorStage.SUBMIT
        );
        ConcurrencyRejectedException rejectedException = new ConcurrencyRejectedException(
                context.getTask().getExecutorName(),
                context.getTask().getTaskName(),
                error,
                rejectedCause
        );

        TaskExecutionEvent event = context.event(AsyncTaskStatus.REJECTED, error, "Task rejected");
        lifecyclePublisher.publish(event);
        finalizeWhenNoFallback(event);
        context.getBaseFuture().completeExceptionally(rejectedException);
    }

    /**
     * 标记排队阶段或等待结果阶段超时。
     *
     * @param throwable 超时异常
     * @param stage QUEUE 表示排队超时；WAIT_RESULT 表示结果层超时
     */
    public boolean completeTimeout(Throwable throwable, AsyncErrorStage stage) {
        TaskExecutionRuntime runtime = context.getRuntime();
        if (!runtime.tryResolveBaseOutcome(AsyncTaskStatus.TIMEOUT)) {
            return false;
        }

        AsyncErrorStage actualStage = stage == null ? AsyncErrorStage.WAIT_RESULT : stage;
        AsyncError error = errorClassifier.classify(
                context.getTask(),
                throwable,
                actualStage
        );
        AsyncTaskException timeoutException = new AsyncTaskException(
                context.getTask().getExecutorName(),
                context.getTask().getTaskName(),
                context.getTask().getTaskId(),
                error,
                throwable
        );

        TaskExecutionEvent event = context.event(
                AsyncTaskStatus.TIMEOUT,
                error,
                actualStage == AsyncErrorStage.QUEUE ? "Task queue timeout" : "Task result timeout"
        );
        lifecyclePublisher.publish(event);
        finalizeWhenNoFallback(event);
        context.getBaseFuture().completeExceptionally(timeoutException);
        return true;
    }

    /**
     * 标记任务被主动取消。
     *
     * @param throwable 取消原因
     * @param mayInterruptIfRunning 是否尽力中断运行线程
     */
    public void completeCancelled(Throwable throwable, boolean mayInterruptIfRunning) {
        TaskExecutionRuntime runtime = context.getRuntime();
        if (!runtime.tryResolveBaseOutcome(AsyncTaskStatus.CANCELLED)) {
            return;
        }

        Throwable cancellation = throwable == null
                ? new CancellationException("Task cancelled")
                : throwable;
        AsyncError error = errorClassifier.classify(
                context.getTask(),
                cancellation,
                AsyncErrorStage.CANCEL
        );
        TaskExecutionEvent event = context.event(
                AsyncTaskStatus.CANCELLED,
                error,
                "Task cancelled"
        );

        runtime.interruptIfNecessary(mayInterruptIfRunning);
        lifecyclePublisher.publish(event);
        finalizeWhenNoFallback(event);
        context.getBaseFuture().cancel(mayInterruptIfRunning);
    }

    /**
     * 超时后只尝试中断底层线程，不把 TIMEOUT 状态覆盖成 CANCELLED。
     *
     * @param mayInterruptIfRunning 是否发送中断信号
     */
    public void interruptRunningIfNecessary(boolean mayInterruptIfRunning) {
        context.getRuntime().interruptIfNecessary(mayInterruptIfRunning);
    }

    /**
     * 完成原始任务成功结果。
     */
    private void completeSuccess(T value) {
        TaskExecutionRuntime runtime = context.getRuntime();
        if (!runtime.tryResolveBaseOutcome(AsyncTaskStatus.SUCCESS)) {
            return;
        }

        runtime.tryFinalize(AsyncTaskStatus.SUCCESS);
        TaskExecutionEvent event = context.event(
                AsyncTaskStatus.SUCCESS,
                AsyncError.none(),
                "Task success"
        );
        lifecyclePublisher.publish(event);
        lifecyclePublisher.publishCompleted(event);
        context.getBaseFuture().complete(value);
    }

    /**
     * 完成原始任务失败结果。
     */
    private void completeFailure(Throwable throwable) {
        TaskExecutionRuntime runtime = context.getRuntime();
        if (!runtime.tryResolveBaseOutcome(AsyncTaskStatus.FAILED)) {
            return;
        }

        AsyncError error = errorClassifier.classify(
                context.getTask(),
                throwable,
                AsyncErrorStage.RUN
        );
        AsyncTaskException taskException = new AsyncTaskException(
                context.getTask().getExecutorName(),
                context.getTask().getTaskName(),
                context.getTask().getTaskId(),
                error,
                throwable
        );
        TaskExecutionEvent event = context.event(
                AsyncTaskStatus.FAILED,
                error,
                "Task failed"
        );

        lifecyclePublisher.publish(event);
        finalizeWhenNoFallback(event);

        if (runtime.isFireAndForget()) {
            uncaughtExceptionHandler.handleException(event.copy(), taskException);
        }
        context.getBaseFuture().completeExceptionally(taskException);
    }

    /**
     * 原始异常状态不存在 fallback 时，直接确定整个任务管道的最终状态。
     */
    private void finalizeWhenNoFallback(TaskExecutionEvent baseTerminalEvent) {
        if (context.hasFallback() && context.getRuntime().isResultAware()) {
            return;
        }

        if (context.getRuntime().tryFinalize(baseTerminalEvent.getStatus())) {
            TaskExecutionEvent finalEvent = context.event(
                    baseTerminalEvent.getStatus(),
                    baseTerminalEvent.getError(),
                    baseTerminalEvent.getMessage()
            );
            lifecyclePublisher.publishCompleted(finalEvent);
        }
    }
}
