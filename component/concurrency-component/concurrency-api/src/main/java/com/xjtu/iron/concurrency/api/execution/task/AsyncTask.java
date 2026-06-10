package com.xjtu.iron.concurrency.api.execution.task;

import com.xjtu.iron.concurrency.api.retry.RetryPolicy;

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
 * <p>用于描述一个完整的异步任务，包括线程池名称、任务名称、业务标识、超时、fallback、上下文传播等。</p>
 *
 * @param <T> 返回值类型
 */
public class AsyncTask<T> {

    /** 任务唯一 ID，不传时自动生成。 */
    private String taskId;

    /** 线程池名称。 */
    private String executorName;

    /** 任务名称，用于日志、指标、排查问题。 */
    private String taskName;

    /** 业务标识，例如 orderId=xxx、userId=xxx。 */
    private String bizKey;

    /** 任务描述。 */
    private String description;

    /** 任务标签，适合存放 scene/source/tenant 等低基数字段。 */
    private Map<String, String> tags = new LinkedHashMap<>();

    /** 结果层超时时间。注意：不保证强制中断底层正在执行的任务。 */
    private Duration timeout;

    /** 排队超时时间。任务在线程池队列里等待超过该时间后，不再执行任务逻辑。 */
    private Duration queueTimeout;

    /** 结果层超时后是否尝试取消底层任务。 */
    private boolean cancelOnTimeout = false;

    /** 取消底层任务时是否尝试中断执行线程。Java 中断是协作式的，不是强杀。 */
    private boolean interruptOnCancel = false;

    /** 任务执行逻辑。 */
    private Supplier<T> supplier;

    /** 任务失败或超时后的 fallback 逻辑。 */
    private Function<Throwable, T> fallback;

    /** 是否开启上下文传播，默认开启。 */
    private boolean contextPropagation = true;

    /** 重试策略占位。一期只作为元数据保留，不在并行组件内部执行复杂重试。 */
    private RetryPolicy retryPolicy = RetryPolicy.none();

    /**
     * 创建异步任务。
     */
    public static <T> AsyncTask<T> of(String executorName, String taskName, Supplier<T> supplier) {
        AsyncTask<T> task = new AsyncTask<>();
        task.taskId = UUID.randomUUID().toString();
        task.executorName = executorName;
        task.taskName = taskName;
        task.supplier = supplier;
        return task;
    }

    public AsyncTask<T> taskId(String taskId) {
        this.taskId = taskId;
        return this;
    }

    public AsyncTask<T> bizKey(String bizKey) {
        this.bizKey = bizKey;
        return this;
    }

    public AsyncTask<T> description(String description) {
        this.description = description;
        return this;
    }

    public AsyncTask<T> tag(String key, String value) {
        if (this.tags == null) {
            this.tags = new LinkedHashMap<>();
        }
        this.tags.put(key, value);
        return this;
    }

    public AsyncTask<T> tags(Map<String, String> tags) {
        this.tags = tags == null ? new LinkedHashMap<>() : new LinkedHashMap<>(tags);
        return this;
    }

    public AsyncTask<T> timeout(Duration timeout) {
        this.timeout = timeout;
        return this;
    }

    public AsyncTask<T> queueTimeout(Duration queueTimeout) {
        this.queueTimeout = queueTimeout;
        return this;
    }

    public AsyncTask<T> cancelOnTimeout(boolean cancelOnTimeout) {
        this.cancelOnTimeout = cancelOnTimeout;
        return this;
    }

    public AsyncTask<T> interruptOnCancel(boolean interruptOnCancel) {
        this.interruptOnCancel = interruptOnCancel;
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

    public AsyncTask<T> retryPolicy(RetryPolicy retryPolicy) {
        this.retryPolicy = retryPolicy == null ? RetryPolicy.none() : retryPolicy;
        return this;
    }

    /**
     * 校验任务配置。
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
        Objects.requireNonNull(supplier, "supplier must not be null");
        if (timeout != null && timeout.isNegative()) {
            throw new IllegalArgumentException("timeout must not be negative");
        }
        if (queueTimeout != null && queueTimeout.isNegative()) {
            throw new IllegalArgumentException("queueTimeout must not be negative");
        }
        if (retryPolicy != null) {
            retryPolicy.validate();
        }
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
        if (tags == null) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(tags);
    }

    public void setTags(Map<String, String> tags) {
        this.tags = tags == null ? new LinkedHashMap<>() : new LinkedHashMap<>(tags);
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

    public RetryPolicy getRetryPolicy() {
        return retryPolicy;
    }

    public void setRetryPolicy(RetryPolicy retryPolicy) {
        this.retryPolicy = retryPolicy;
    }
}
