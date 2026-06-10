package com.xjtu.iron.concurrency.api.event;

import com.xjtu.iron.concurrency.api.enums.task.AsyncTaskStatus;
import com.xjtu.iron.concurrency.api.error.AsyncError;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 任务执行事件。
 *
 * <p>
 * 用于向任务监听器、任务状态注册表、指标记录器传递任务执行过程中的状态、耗时、错误和上下文信息。
 * </p>
 */
public class TaskExecutionEvent {

    /**
     * 任务唯一 ID。
     */
    private String taskId;

    /**
     * 线程池名称。
     */
    private String executorName;

    /**
     * 任务名称。
     */
    private String taskName;

    /**
     * 业务标识。
     *
     * <p>
     * 例如 orderId=xxx、userId=xxx、batchNo=xxx。
     * 该字段适合用于排查和补偿，不建议作为指标 tag。
     * </p>
     */
    private String bizKey;

    /**
     * 任务描述。
     */
    private String description;

    /**
     * 事件说明。
     */
    private String message;

    /**
     * 任务标签。
     *
     * <p>
     * 适合存放 scene、source、tenant 等低基数字段。
     * </p>
     */
    private Map<String, String> tags = new LinkedHashMap<>();

    /**
     * 任务状态。
     */
    private AsyncTaskStatus status;

    /**
     * 异步错误详情。
     *
     * <p>
     * 成功任务可以为 AsyncError.none()。
     * 失败、拒绝、超时、取消、fallback 失败时记录具体错误。
     * </p>
     */
    private AsyncError error = AsyncError.none();

    /**
     * 任务提交时间戳，毫秒。
     */
    private long submitTimeMillis;

    /**
     * 任务开始执行时间戳，毫秒。
     */
    private long startTimeMillis;

    /**
     * 任务结束时间戳，毫秒。
     */
    private long endTimeMillis;

    /**
     * 排队耗时，毫秒。
     */
    private long queueCostMillis;

    /**
     * 实际执行耗时，毫秒。
     */
    private long runCostMillis;

    /**
     * 从提交到结束的总耗时，毫秒。
     */
    private long totalCostMillis;

    /**
     * 是否 fire-and-forget 任务。
     */
    private boolean fireAndForget;

    /**
     * 事件创建时间。
     */
    private Instant eventTime = Instant.now();

    public TaskExecutionEvent copy() {
        TaskExecutionEvent event = new TaskExecutionEvent();
        event.taskId = taskId;
        event.executorName = executorName;
        event.taskName = taskName;
        event.bizKey = bizKey;
        event.description = description;
        event.message = message;
        event.tags = tags == null ? new LinkedHashMap<>() : new LinkedHashMap<>(tags);
        event.status = status;
        event.error = error == null ? AsyncError.none() : error.copy();
        event.submitTimeMillis = submitTimeMillis;
        event.startTimeMillis = startTimeMillis;
        event.endTimeMillis = endTimeMillis;
        event.queueCostMillis = queueCostMillis;
        event.runCostMillis = runCostMillis;
        event.totalCostMillis = totalCostMillis;
        event.fireAndForget = fireAndForget;
        event.eventTime = eventTime;
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

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
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

    public AsyncError getError() {
        return error;
    }

    public void setError(AsyncError error) {
        this.error = error == null ? AsyncError.none() : error;
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
}