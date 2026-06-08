package com.xjtu.iron.concurrency.api.event;

import com.xjtu.iron.concurrency.api.enums.AsyncTaskStatus;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 任务执行事件。
 *
 * <p>这个对象用于在任务生命周期中向监听器传递任务元数据、执行状态、耗时与异常。</p>
 *
 * <p>它不是业务返回值，也不是线程池快照，而是一次具体异步任务在某个阶段的事件描述。</p>
 */
public class TaskExecutionEvent {

    /**
     * 任务唯一标识。
     */
    private final String taskId;

    /**
     * 线程池名称。
     */
    private final String executorName;

    /**
     * 任务名称。
     */
    private final String taskName;

    /**
     * 业务标识，例如 orderId=10001、userId=20002。
     */
    private final String bizKey;

    /**
     * 任务描述。
     */
    private final String description;

    /**
     * 任务标签。
     */
    private final Map<String, String> tags;

    /**
     * 当前事件对应的任务状态。
     */
    private final AsyncTaskStatus status;

    /**
     * 任务提交时间，单位毫秒。
     */
    private final long submitTimeMillis;

    /**
     * 任务开始执行时间，单位毫秒。尚未开始时为 0。
     */
    private final long startTimeMillis;

    /**
     * 任务结束时间，单位毫秒。尚未结束时为 0。
     */
    private final long endTimeMillis;

    /**
     * 排队耗时，单位毫秒。
     */
    private final long queueCostMillis;

    /**
     * 执行耗时，单位毫秒。
     */
    private final long runCostMillis;

    /**
     * 总耗时，单位毫秒。
     */
    private final long totalCostMillis;

    /**
     * 异常对象。成功事件时为空。
     */
    private final Throwable throwable;

    /**
     * 事件说明。
     */
    private final String message;

    private TaskExecutionEvent(Builder builder) {
        this.taskId = builder.taskId;
        this.executorName = builder.executorName;
        this.taskName = builder.taskName;
        this.bizKey = builder.bizKey;
        this.description = builder.description;
        this.tags = Collections.unmodifiableMap(new LinkedHashMap<>(builder.tags));
        this.status = builder.status;
        this.submitTimeMillis = builder.submitTimeMillis;
        this.startTimeMillis = builder.startTimeMillis;
        this.endTimeMillis = builder.endTimeMillis;
        this.queueCostMillis = builder.queueCostMillis;
        this.runCostMillis = builder.runCostMillis;
        this.totalCostMillis = builder.totalCostMillis;
        this.throwable = builder.throwable;
        this.message = builder.message;
    }

    /**
     * 创建事件构造器。
     *
     * @return 事件构造器
     */
    public static Builder builder() {
        return new Builder();
    }

    public String getTaskId() {
        return taskId;
    }

    public String getExecutorName() {
        return executorName;
    }

    public String getTaskName() {
        return taskName;
    }

    public String getBizKey() {
        return bizKey;
    }

    public String getDescription() {
        return description;
    }

    public Map<String, String> getTags() {
        return tags;
    }

    public AsyncTaskStatus getStatus() {
        return status;
    }

    public long getSubmitTimeMillis() {
        return submitTimeMillis;
    }

    public long getStartTimeMillis() {
        return startTimeMillis;
    }

    public long getEndTimeMillis() {
        return endTimeMillis;
    }

    public long getQueueCostMillis() {
        return queueCostMillis;
    }

    public long getRunCostMillis() {
        return runCostMillis;
    }

    public long getTotalCostMillis() {
        return totalCostMillis;
    }

    public Throwable getThrowable() {
        return throwable;
    }

    public String getMessage() {
        return message;
    }

    /**
     * 任务执行事件构造器。
     */
    public static class Builder {

        private String taskId;
        private String executorName;
        private String taskName;
        private String bizKey;
        private String description;
        private Map<String, String> tags = new LinkedHashMap<>();
        private AsyncTaskStatus status;
        private long submitTimeMillis;
        private long startTimeMillis;
        private long endTimeMillis;
        private long queueCostMillis;
        private long runCostMillis;
        private long totalCostMillis;
        private Throwable throwable;
        private String message;

        public Builder taskId(String taskId) {
            this.taskId = taskId;
            return this;
        }

        public Builder executorName(String executorName) {
            this.executorName = executorName;
            return this;
        }

        public Builder taskName(String taskName) {
            this.taskName = taskName;
            return this;
        }

        public Builder bizKey(String bizKey) {
            this.bizKey = bizKey;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder tags(Map<String, String> tags) {
            this.tags = tags == null ? new LinkedHashMap<>() : new LinkedHashMap<>(tags);
            return this;
        }

        public Builder status(AsyncTaskStatus status) {
            this.status = status;
            return this;
        }

        public Builder submitTimeMillis(long submitTimeMillis) {
            this.submitTimeMillis = submitTimeMillis;
            return this;
        }

        public Builder startTimeMillis(long startTimeMillis) {
            this.startTimeMillis = startTimeMillis;
            return this;
        }

        public Builder endTimeMillis(long endTimeMillis) {
            this.endTimeMillis = endTimeMillis;
            return this;
        }

        public Builder queueCostMillis(long queueCostMillis) {
            this.queueCostMillis = queueCostMillis;
            return this;
        }

        public Builder runCostMillis(long runCostMillis) {
            this.runCostMillis = runCostMillis;
            return this;
        }

        public Builder totalCostMillis(long totalCostMillis) {
            this.totalCostMillis = totalCostMillis;
            return this;
        }

        public Builder throwable(Throwable throwable) {
            this.throwable = throwable;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public TaskExecutionEvent build() {
            return new TaskExecutionEvent(this);
        }
    }
}
