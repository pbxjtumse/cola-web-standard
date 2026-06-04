package com.xjtu.iron.concurrency.api.execution;

import java.time.Duration;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 异步任务模型。
 *
 * <p>这个类不是业务领域模型，而是并行组件内部用于描述“一个异步任务如何被执行”的任务模型。</p>
 *
 * <p>它比 {@link AsyncExecutor#supply(String, String, Supplier)} 更完整，适合需要 timeout、fallback、上下文传播控制的复杂任务。</p>
 *
 * @param <T> 任务返回值类型
 */
public class AsyncTask<T> {

    /**
     * 线程池名称。
     *
     * <p>通过这个名称从 ThreadPoolRegistry 中找到对应的 ThreadPoolExecutor。</p>
     */
    private String executorName;

    /**
     * 任务名称。
     *
     * <p>用于日志、指标、异常排查、诊断展示。</p>
     */
    private String taskName;

    /**
     * 任务结果层超时时间。
     *
     * <p>注意：一期这里是 CompletableFuture 结果层超时，不保证强制中断底层正在执行的线程。</p>
     */
    private Duration timeout;

    /**
     * 任务执行逻辑。
     *
     * <p>真正的业务代码会被包装成 TaskCommand 后提交给 ThreadPoolExecutor。</p>
     */
    private Supplier<T> supplier;

    /**
     * fallback 降级逻辑。
     *
     * <p>当任务执行异常或结果层超时时，如果配置了 fallback，则返回 fallback 结果。</p>
     */
    private Function<Throwable, T> fallback;

    /**
     * 是否开启上下文传播。
     *
     * <p>默认开启。关闭后不会通过 ContextAwareTaskDecorator 捕获和恢复上下文。</p>
     */
    private boolean contextPropagation = true;

    /**
     * 创建一个基础异步任务。
     *
     * @param executorName 线程池名称
     * @param taskName 任务名称
     * @param supplier 任务执行逻辑
     * @param <T> 返回值类型
     * @return 异步任务模型
     */
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

    /**
     * 设置任务超时时间。
     *
     * @param timeout 结果层超时时间
     * @return 当前任务模型
     */
    public AsyncTask<T> timeout(Duration timeout) {
        this.timeout = timeout;
        return this;
    }

    /**
     * 设置任务 fallback。
     *
     * @param fallback 降级函数
     * @return 当前任务模型
     */
    public AsyncTask<T> fallback(Function<Throwable, T> fallback) {
        this.fallback = fallback;
        return this;
    }

    /**
     * 设置是否传播上下文。
     *
     * @param contextPropagation 是否传播上下文
     * @return 当前任务模型
     */
    public AsyncTask<T> contextPropagation(boolean contextPropagation) {
        this.contextPropagation = contextPropagation;
        return this;
    }

    /**
     * 校验任务模型是否合法。
     */
    public void validate() {
        if (executorName == null || executorName.isBlank()) {
            throw new IllegalArgumentException("executorName must not be blank");
        }

        if (taskName == null || taskName.isBlank()) {
            throw new IllegalArgumentException("taskName must not be blank");
        }

        Objects.requireNonNull(supplier, "supplier must not be null");
    }

    /**
     * 获取线程池名称。
     *
     * @return 线程池名称
     */
    public String getExecutorName() {
        return executorName;
    }

    /**
     * 设置线程池名称。
     *
     * @param executorName 线程池名称
     */
    public void setExecutorName(String executorName) {
        this.executorName = executorName;
    }

    /**
     * 获取任务名称。
     *
     * @return 任务名称
     */
    public String getTaskName() {
        return taskName;
    }

    /**
     * 设置任务名称。
     *
     * @param taskName 任务名称
     */
    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    /**
     * 获取任务超时时间。
     *
     * @return 任务超时时间
     */
    public Duration getTimeout() {
        return timeout;
    }

    /**
     * 设置任务超时时间。
     *
     * @param timeout 任务超时时间
     */
    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }

    /**
     * 获取任务执行逻辑。
     *
     * @return 任务执行逻辑
     */
    public Supplier<T> getSupplier() {
        return supplier;
    }

    /**
     * 设置任务执行逻辑。
     *
     * @param supplier 任务执行逻辑
     */
    public void setSupplier(Supplier<T> supplier) {
        this.supplier = supplier;
    }

    /**
     * 获取 fallback 函数。
     *
     * @return fallback 函数
     */
    public Function<Throwable, T> getFallback() {
        return fallback;
    }

    /**
     * 设置 fallback 函数。
     *
     * @param fallback fallback 函数
     */
    public void setFallback(Function<Throwable, T> fallback) {
        this.fallback = fallback;
    }

    /**
     * 判断是否开启上下文传播。
     *
     * @return true 表示开启上下文传播
     */
    public boolean isContextPropagation() {
        return contextPropagation;
    }

    /**
     * 设置是否开启上下文传播。
     *
     * @param contextPropagation 是否开启上下文传播
     */
    public void setContextPropagation(boolean contextPropagation) {
        this.contextPropagation = contextPropagation;
    }
}
