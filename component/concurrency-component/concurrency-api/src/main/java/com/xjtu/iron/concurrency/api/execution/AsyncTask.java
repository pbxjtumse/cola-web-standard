package com.xjtu.iron.concurrency.api.execution;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 异步任务模型。
 *
 * <p>这个类不是业务领域模型，而是并行组件内部用于描述“一个异步任务如何被执行”的任务模型。</p>
 *
 * <p>它比 {@link AsyncExecutor#supply(String, String, Supplier)} 更完整，适合需要 timeout、fallback、上下文传播、任务元数据、排队超时、取消策略的复杂任务。</p>
 *
 * @param <T> 任务返回值类型
 */
public class AsyncTask<T> {

    /**
     * 任务唯一标识。
     *
     * <p>默认由组件自动生成。业务也可以主动设置，用于日志关联、诊断追踪、任务状态查询。</p>
     */
    private String taskId;

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
     * 业务标识。
     *
     * <p>例如 orderId=10001、userId=20002、batchNo=20260608。这个字段不参与执行逻辑，只用于排查和观测。</p>
     */
    private String bizKey;

    /**
     * 任务描述。
     */
    private String description;

    /**
     * 任务标签。
     *
     * <p>建议放 scene、source、tenant、priority 等低基数字段。不要放订单号、用户号这种高基数字段到指标标签里。</p>
     */
    private Map<String, String> tags = new LinkedHashMap<>();

    /**
     * 任务结果层超时时间。
     *
     * <p>注意：一期这里是 CompletableFuture 结果层超时，不天然等价于强制中断底层线程。</p>
     */
    private Duration timeout;

    /**
     * 排队超时时间。
     *
     * <p>任务在线程池队列中等待超过该时间后，即使被工作线程取出，也会被标记为超时并不再执行 supplier。</p>
     */
    private Duration queueTimeout;

    /**
     * 结果层超时后是否尝试取消底层任务。
     *
     * <p>取消是尽力而为。Java 线程中断是协作式机制，不能保证一定停止业务代码。</p>
     */
    private boolean cancelOnTimeout = false;

    /**
     * 取消底层任务时是否尝试中断运行线程。
     *
     * <p>只有 {@link #cancelOnTimeout} 为 true 时才有意义。</p>
     */
    private boolean interruptOnCancel = false;

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
        task.taskId = UUID.randomUUID().toString();
        task.executorName = executorName;
        task.taskName = taskName;
        task.supplier = supplier;
        return task;
    }

    /**
     * 设置任务唯一标识。
     *
     * @param taskId 任务唯一标识
     * @return 当前任务模型
     */
    public AsyncTask<T> taskId(String taskId) {
        this.taskId = taskId;
        return this;
    }

    /**
     * 设置业务标识。
     *
     * @param bizKey 业务标识
     * @return 当前任务模型
     */
    public AsyncTask<T> bizKey(String bizKey) {
        this.bizKey = bizKey;
        return this;
    }

    /**
     * 设置任务描述。
     *
     * @param description 任务描述
     * @return 当前任务模型
     */
    public AsyncTask<T> description(String description) {
        this.description = description;
        return this;
    }

    /**
     * 添加一个任务标签。
     *
     * @param key 标签名
     * @param value 标签值
     * @return 当前任务模型
     */
    public AsyncTask<T> tag(String key, String value) {
        if (key != null && !key.isBlank()) {
            this.tags.put(key, value);
        }
        return this;
    }

    /**
     * 批量设置任务标签。
     *
     * @param tags 任务标签
     * @return 当前任务模型
     */
    public AsyncTask<T> tags(Map<String, String> tags) {
        this.tags.clear();
        if (tags != null) {
            this.tags.putAll(tags);
        }
        return this;
    }

    /**
     * 设置任务结果层超时时间。
     *
     * @param timeout 结果层超时时间
     * @return 当前任务模型
     */
    public AsyncTask<T> timeout(Duration timeout) {
        this.timeout = timeout;
        return this;
    }

    /**
     * 设置任务排队超时时间。
     *
     * @param queueTimeout 排队超时时间
     * @return 当前任务模型
     */
    public AsyncTask<T> queueTimeout(Duration queueTimeout) {
        this.queueTimeout = queueTimeout;
        return this;
    }

    /**
     * 设置结果层超时后是否尝试取消底层任务。
     *
     * @param cancelOnTimeout 是否取消
     * @return 当前任务模型
     */
    public AsyncTask<T> cancelOnTimeout(boolean cancelOnTimeout) {
        this.cancelOnTimeout = cancelOnTimeout;
        return this;
    }

    /**
     * 设置取消时是否尝试中断运行线程。
     *
     * @param interruptOnCancel 是否中断
     * @return 当前任务模型
     */
    public AsyncTask<T> interruptOnCancel(boolean interruptOnCancel) {
        this.interruptOnCancel = interruptOnCancel;
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
        if (taskId == null || taskId.isBlank()) {
            taskId = UUID.randomUUID().toString();
        }

        if (executorName == null || executorName.isBlank()) {
            throw new IllegalArgumentException("executorName must not be blank");
        }

        if (taskName == null || taskName.isBlank()) {
            throw new IllegalArgumentException("taskName must not be blank");
        }

        if (timeout != null && (timeout.isZero() || timeout.isNegative())) {
            throw new IllegalArgumentException("timeout must be positive");
        }

        if (queueTimeout != null && (queueTimeout.isZero() || queueTimeout.isNegative())) {
            throw new IllegalArgumentException("queueTimeout must be positive");
        }

        Objects.requireNonNull(supplier, "supplier must not be null");
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
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

    public String getBizKey() {
        return bizKey;
    }

    public void setBizKey(String bizKey) {
        this.bizKey = bizKey;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, String> getTags() {
        return Collections.unmodifiableMap(tags);
    }

    public void setTags(Map<String, String> tags) {
        this.tags.clear();
        if (tags != null) {
            this.tags.putAll(tags);
        }
    }

    public Duration getTimeout() {
        return timeout;
    }

    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }

    public Duration getQueueTimeout() {
        return queueTimeout;
    }

    public void setQueueTimeout(Duration queueTimeout) {
        this.queueTimeout = queueTimeout;
    }

    public boolean isCancelOnTimeout() {
        return cancelOnTimeout;
    }

    public void setCancelOnTimeout(boolean cancelOnTimeout) {
        this.cancelOnTimeout = cancelOnTimeout;
    }

    public boolean isInterruptOnCancel() {
        return interruptOnCancel;
    }

    public void setInterruptOnCancel(boolean interruptOnCancel) {
        this.interruptOnCancel = interruptOnCancel;
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
