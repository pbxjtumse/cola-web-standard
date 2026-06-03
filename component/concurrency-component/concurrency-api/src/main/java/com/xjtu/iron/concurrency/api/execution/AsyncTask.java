package com.xjtu.iron.concurrency.api.execution;


import java.time.Duration;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 异步任务模型。
 *
 * <p>用于描述一个完整的异步任务。</p>
 *
 * @param <T> 返回值类型
 */
public class AsyncTask<T> {

    /**
     * 线程池名称。
     */
    private String executorName;

    /**
     * 任务名称。
     */
    private String taskName;

    /**
     * 任务超时时间。
     *
     * <p>注意：P1 的超时是结果层超时，不保证强制中断底层正在执行的任务。</p>
     */
    private Duration timeout;

    /**
     * 任务执行逻辑。
     */
    private Supplier<T> supplier;

    /**
     * fallback 逻辑。
     *
     * <p>当任务执行异常或超时时，如果配置了 fallback，则返回 fallback 结果。</p>
     */
    private Function<Throwable, T> fallback;

    /**
     * 是否开启上下文传播。
     *
     * <p>默认开启。</p>
     */
    private boolean contextPropagation = true;

    public static <T> AsyncTask<T> of(
            String executorName,
            String taskName,
            Supplier<T> supplier
    ) {
        AsyncTask<T> task = new AsyncTask<>();
        task.executorName = executorName;
        task.taskName = taskName;
        task.supplier = supplier;
        return task;
    }

    public AsyncTask<T> timeout(Duration timeout) {
        this.timeout = timeout;
        return this;
    }

    public AsyncTask<T> fallback(Function<Throwable, T> fallback) {
        this.fallback = fallback;
        return this;
    }

    public AsyncTask<T> contextPropagation(boolean contextPropagation) {
        this.contextPropagation = contextPropagation;
        return this;
    }

    public void validate() {
        if (executorName == null || executorName.isBlank()) {
            throw new IllegalArgumentException("executorName must not be blank");
        }

        if (taskName == null || taskName.isBlank()) {
            throw new IllegalArgumentException("taskName must not be blank");
        }

        Objects.requireNonNull(supplier, "supplier must not be null");
    }

    public String getExecutorName() {
        return executorName;
    }

    public void setExecutorName(String executorName) {
        this.executorName = executorName;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }

    public Supplier<T> getSupplier() {
        return supplier;
    }

    public void setSupplier(Supplier<T> supplier) {
        this.supplier = supplier;
    }

    public Function<Throwable, T> getFallback() {
        return fallback;
    }

    public void setFallback(Function<Throwable, T> fallback) {
        this.fallback = fallback;
    }

    public boolean isContextPropagation() {
        return contextPropagation;
    }

    public void setContextPropagation(boolean contextPropagation) {
        this.contextPropagation = contextPropagation;
    }
}
