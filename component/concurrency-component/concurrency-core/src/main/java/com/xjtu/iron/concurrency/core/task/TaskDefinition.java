package com.xjtu.iron.concurrency.core.task;

import com.xjtu.iron.concurrency.api.retry.RetryPolicy;
import com.xjtu.iron.concurrency.api.task.TaskMetadata;

import java.time.Duration;
import java.util.function.Function;
import java.util.function.Supplier;

public final class TaskDefinition<T> {

    /**
     * 固定任务元数据。
     */
    private final TaskMetadata metadata;

    /**
     * 用户原始执行逻辑。
     */
    private final Supplier<T> operation;

    /**
     * 结果超时。
     */
    private final Duration timeout;

    /**
     * 排队超时。
     */
    private final Duration queueTimeout;

    /**
     * 超时后是否取消。
     */
    private final boolean cancelOnTimeout;

    /**
     * 超时取消时是否中断。
     */
    private final boolean interruptOnTimeout;

    /**
     * fallback 逻辑。
     */
    private final Function<Throwable, T> fallback;

    /**
     * 是否传播上下文。
     */
    private final boolean contextPropagation;

    /**
     * 重试策略快照。
     */
    private final RetryPolicy retryPolicy;

    public TaskDefinition(TaskMetadata metadata,
                          Supplier<T> operation,
                          Duration timeout,
                          Duration queueTimeout,
                          boolean cancelOnTimeout,
                          boolean interruptOnTimeout,
                          Function<Throwable, T> fallback,
                          boolean contextPropagation,
                          RetryPolicy retryPolicy) {
        this.metadata = metadata;
        this.operation = operation;
        this.timeout = timeout;
        this.queueTimeout = queueTimeout;
        this.cancelOnTimeout = cancelOnTimeout;
        this.interruptOnTimeout = interruptOnTimeout;
        this.fallback = fallback;
        this.contextPropagation = contextPropagation;
        this.retryPolicy = retryPolicy;
    }

    public TaskMetadata getMetadata() {
        return metadata;
    }

    public Supplier<T> getOperation() {
        return operation;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public Duration getQueueTimeout() {
        return queueTimeout;
    }

    public boolean isCancelOnTimeout() {
        return cancelOnTimeout;
    }

    public boolean isInterruptOnTimeout() {
        return interruptOnTimeout;
    }

    public Function<Throwable, T> getFallback() {
        return fallback;
    }

    public boolean isContextPropagation() {
        return contextPropagation;
    }

    public RetryPolicy getRetryPolicy() {
        return retryPolicy;
    }
}
