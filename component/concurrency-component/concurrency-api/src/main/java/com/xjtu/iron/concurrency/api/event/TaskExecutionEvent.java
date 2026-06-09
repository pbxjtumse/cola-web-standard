package com.xjtu.iron.concurrency.api.event;

import com.xjtu.iron.concurrency.api.enums.AsyncTaskStatus;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 任务执行事件。对外事件模型，监听器参数
 *
 * <p>该对象用于监听器、指标记录器、异常处理器之间传递任务状态和耗时数据。</p>
 */
public class TaskExecutionEvent {

    /** 任务唯一 ID。 */
    private String taskId;

    /** 线程池名称。 */
    private String executorName;

    /** 任务名称。 */
    private String taskName;

    /** 业务标识，例如 orderId=xxx、userId=xxx。 */
    private String bizKey;

    /** 任务描述。 */
    private String description;

    /** 任务标签。 */
    private Map<String, String> tags = new LinkedHashMap<>();

    /** 当前任务状态。 */
    private AsyncTaskStatus status;

    /** 提交时间戳，毫秒。 */
    private long submitTimeMillis;

    /** 开始执行时间戳，毫秒。 */
    private long startTimeMillis;

    /** 结束时间戳，毫秒。 */
    private long endTimeMillis;

    /** 排队耗时，毫秒。 */
    private long queueCostMillis;

    /** 实际执行耗时，毫秒。 */
    private long runCostMillis;

    /** 从提交到完成的总耗时，毫秒。 */
    private long totalCostMillis;

    /** 异常对象。 */
    private Throwable throwable;

    /** 是否 fire-and-forget 任务。 */
    private boolean fireAndForget;

    /** 事件发生时间。 */
    private Instant eventTime = Instant.now();

    /** 事件说明，例如 submitted、started、failed、timeout 等补充说明。 */
    private String message;

    /**
     * 创建事件副本，避免监听器修改内部状态。
     */
    public TaskExecutionEvent copy() {
        TaskExecutionEvent event = new TaskExecutionEvent();
        event.taskId = taskId;
        event.executorName = executorName;
        event.taskName = taskName;
        event.bizKey = bizKey;
        event.description = description;
        event.tags = tags == null ? new LinkedHashMap<>() : new LinkedHashMap<>(tags);
        event.status = status;
        event.submitTimeMillis = submitTimeMillis;
        event.startTimeMillis = startTimeMillis;
        event.endTimeMillis = endTimeMillis;
        event.queueCostMillis = queueCostMillis;
        event.runCostMillis = runCostMillis;
        event.totalCostMillis = totalCostMillis;
        event.throwable = throwable;
        event.fireAndForget = fireAndForget;
        event.eventTime = eventTime;
        event.message = message;
        return event;
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

    public AsyncTaskStatus getStatus() {
        return status;
    }

    public void setStatus(AsyncTaskStatus status) {
        this.status = status;
    }

    public long getSubmitTimeMillis() {
        return submitTimeMillis;
    }

    public void setSubmitTimeMillis(long submitTimeMillis) {
        this.submitTimeMillis = submitTimeMillis;
    }

    public long getStartTimeMillis() {
        return startTimeMillis;
    }

    public void setStartTimeMillis(long startTimeMillis) {
        this.startTimeMillis = startTimeMillis;
    }

    public long getEndTimeMillis() {
        return endTimeMillis;
    }

    public void setEndTimeMillis(long endTimeMillis) {
        this.endTimeMillis = endTimeMillis;
    }

    public long getQueueCostMillis() {
        return queueCostMillis;
    }

    public void setQueueCostMillis(long queueCostMillis) {
        this.queueCostMillis = queueCostMillis;
    }

    public long getRunCostMillis() {
        return runCostMillis;
    }

    public void setRunCostMillis(long runCostMillis) {
        this.runCostMillis = runCostMillis;
    }

    public long getTotalCostMillis() {
        return totalCostMillis;
    }

    public void setTotalCostMillis(long totalCostMillis) {
        this.totalCostMillis = totalCostMillis;
    }

    public Throwable getThrowable() {
        return throwable;
    }

    public void setThrowable(Throwable throwable) {
        this.throwable = throwable;
    }

    public boolean isFireAndForget() {
        return fireAndForget;
    }

    public void setFireAndForget(boolean fireAndForget) {
        this.fireAndForget = fireAndForget;
    }

    public Instant getEventTime() {
        return eventTime;
    }

    public void setEventTime(Instant eventTime) {
        this.eventTime = eventTime;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
