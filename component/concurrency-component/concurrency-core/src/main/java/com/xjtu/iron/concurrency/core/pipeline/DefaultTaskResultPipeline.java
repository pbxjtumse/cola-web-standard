package com.xjtu.iron.concurrency.core.pipeline;

import com.xjtu.iron.concurrency.api.enums.error.AsyncErrorStage;
import com.xjtu.iron.concurrency.api.enums.task.AsyncTaskStatus;
import com.xjtu.iron.concurrency.api.error.AsyncError;
import com.xjtu.iron.concurrency.api.error.AsyncErrorClassifier;
import com.xjtu.iron.concurrency.api.error.CompletableFutureExceptionUtils;
import com.xjtu.iron.concurrency.api.event.TaskExecutionEvent;
import com.xjtu.iron.concurrency.api.exception.AsyncTaskException;
import com.xjtu.iron.concurrency.api.exception.ConcurrencyException;
import com.xjtu.iron.concurrency.api.execution.template.AsyncTemplate;
import com.xjtu.iron.concurrency.core.lifecycle.TaskLifecyclePublisher;
import com.xjtu.iron.concurrency.core.task.TaskCommand;
import com.xjtu.iron.concurrency.core.task.TaskExecutionContext;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.*;

/**
 * 默认任务结果处理管道。
 *
 * <p>
 * 先应用结果层超时，再应用 fallback。这样超时、原始执行失败和拒绝都可以统一进入 fallback。
 * </p>
 */
public final class DefaultTaskResultPipeline implements TaskResultPipeline {

    /**
     * 组合后的结构化错误分类器。
     */
    private final AsyncErrorClassifier errorClassifier;

    /**
     * 任务生命周期发布器。
     *
     * <p>
     * 用于统一更新指标、任务状态注册表和任务监听器。
     * </p>
     */
    private final TaskLifecyclePublisher lifecyclePublisher;

    /**
     * 结果层超时调度器。
     *
     * <p>
     * 只负责触发短小的超时判断，不负责执行 fallback 或其他业务逻辑。
     * </p>
     */
    private final ScheduledExecutorService timeoutScheduler;

    /**
     * fallback 专用执行器。
     *
     * <p>
     * 避免 fallback 阻塞原始工作线程或超时调度线程。
     * </p>
     */
    private final Executor fallbackExecutor;

    /**
     * 创建默认任务结果处理管道。
     *
     * @param errorClassifier 错误分类器
     * @param lifecyclePublisher 生命周期发布器
     * @param timeoutScheduler 超时调度器
     * @param fallbackExecutor fallback 执行器
     */
    public DefaultTaskResultPipeline(
            AsyncErrorClassifier errorClassifier,
            TaskLifecyclePublisher lifecyclePublisher,
            ScheduledExecutorService timeoutScheduler,
            Executor fallbackExecutor
    ) {
        this.errorClassifier = Objects.requireNonNull(
                errorClassifier,
                "errorClassifier must not be null"
        );
        this.lifecyclePublisher = Objects.requireNonNull(
                lifecyclePublisher,
                "lifecyclePublisher must not be null"
        );
        this.timeoutScheduler = Objects.requireNonNull(
                timeoutScheduler,
                "timeoutScheduler must not be null"
        );
        this.fallbackExecutor = Objects.requireNonNull(
                fallbackExecutor,
                "fallbackExecutor must not be null"
        );
    }


    private <T> CompletableFuture<T> applyTimeout(
            TaskExecutionContext<T> context,
            TaskCommand<T> command,
            CompletableFuture<T> source
    ) {
        Duration timeout = context.getTask().getTimeout();

        /**
         * 返回给后续 fallback 管道使用的新 Future。
         *
         * <p>
         * 不直接修改 source，避免破坏原始任务 Future。
         * </p>
         */
        CompletableFuture<T> result = new CompletableFuture<>();

        /**
         * 注册结果层超时任务。
         */
        ScheduledFuture<?> timeoutFuture = timeoutScheduler.schedule(
                () -> handleTimeout(
                        context,
                        command,
                        result,
                        timeout
                ),
                timeout.toMillis(),
                TimeUnit.MILLISECONDS
        );

        /**
         * 原始任务提前结束时，把结果转发给 result。
         */
        source.whenComplete((value, throwable) -> {
            boolean completed;

            if (throwable == null) {
                completed = result.complete(value);
            } else {
                completed = result.completeExceptionally(
                        CompletableFutureExceptionUtils.unwrap(throwable)
                );
            }

            if (completed) {
                /*
                 * 原始任务先结束，取消尚未触发的超时检查。
                 */
                timeoutFuture.cancel(false);
            }
        });

        return result;
    }

    private <T> void handleTimeout(
            TaskExecutionContext<T> context,
            TaskCommand<T> command,
            CompletableFuture<T> result,
            Duration timeout
    ) {
        /**
         * result 已经完成，说明原始任务先结束，不需要再处理超时。
         */
        if (result.isDone()) {
            return;
        }

        TimeoutException timeoutException = new TimeoutException(
                "Async task result timeout after "
                        + timeout.toMillis()
                        + " ms"
        );

        /**
         * TaskCommand 内部通过 CAS 判断超时是否真正抢到原始任务结果。
         */
        boolean timeoutWon = command.completeTimeout(
                timeoutException,
                AsyncErrorStage.WAIT_RESULT
        );

        /**
         * 只有超时真正生效，才允许尝试中断底层工作线程。
         */
        if (timeoutWon
                && context.getTask().isCancelOnTimeout()) {
            command.interruptRunningIfNecessary(
                    context.getTask().isInterruptOnCancel()
            );
        }
    }

    private <T> CompletableFuture<T> applyFallback(
            TaskExecutionContext<T> context,
            CompletableFuture<T> source
    ) {
        CompletableFuture<T> result = new CompletableFuture<>();

        source.whenComplete((value, throwable) -> {
            if (throwable == null) {
                result.complete(value);
                return;
            }

            AsyncError originalError = resolveOriginalError(
                    context,
                    throwable
            );

            /**
             * fallback 被触发，这是中间状态。
             */
            context.getRuntime().markIntermediate(
                    AsyncTaskStatus.FALLBACK
            );

            lifecyclePublisher.publish(context.event(
                    AsyncTaskStatus.FALLBACK,
                    originalError,
                    "Fallback triggered"
            ));

            try {
                /**
                 * fallback 在线程池中独立执行。
                 */
                fallbackExecutor.execute(() ->
                        executeFallback(
                                context,
                                throwable,
                                originalError,
                                result
                        )
                );
            } catch (RejectedExecutionException fallbackRejected) {
                /**
                 * fallback 线程池自身拒绝，也属于 fallback 失败。
                 */
                completeFallbackFailure(
                        context,
                        fallbackRejected,
                        result,
                        "Fallback executor rejected task"
                );
            }
        });

        return result;
    }


    private <T> void executeFallback(
            TaskExecutionContext<T> context,
            Throwable originalThrowable,
            AsyncError originalError,
            CompletableFuture<T> result
    ) {
        Throwable fallbackInput =
                CompletableFutureExceptionUtils.rootCause(
                        originalThrowable
                );

        if (fallbackInput == null) {
            fallbackInput =
                    CompletableFutureExceptionUtils.unwrap(
                            originalThrowable
                    );
        }

        try {
            T fallbackValue = context.getTask()
                    .getFallback()
                    .apply(fallbackInput);

            if (context.getRuntime().tryFinalize(
                    AsyncTaskStatus.FALLBACK_SUCCESS
            )) {
                TaskExecutionEvent successEvent = context.event(
                        AsyncTaskStatus.FALLBACK_SUCCESS,
                        originalError,
                        "Fallback success"
                );

                lifecyclePublisher.publish(successEvent);
                lifecyclePublisher.publishCompleted(successEvent);
            }

            result.complete(fallbackValue);
        } catch (Throwable fallbackThrowable) {
            completeFallbackFailure(
                    context,
                    fallbackThrowable,
                    result,
                    "Fallback failed"
            );
        }
    }

}
