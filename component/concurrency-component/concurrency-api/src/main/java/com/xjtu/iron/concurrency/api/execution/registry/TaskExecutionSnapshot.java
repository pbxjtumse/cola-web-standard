package com.xjtu.iron.concurrency.api.execution.registry;

import com.xjtu.iron.concurrency.api.enums.task.AsyncTaskStatus;
import com.xjtu.iron.concurrency.api.error.AsyncError;
import com.xjtu.iron.concurrency.api.event.TaskExecutionEvent;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 任务状态快照。
 *
 * <p>
 * 用于任务状态注册表保存最近任务状态。该对象面向查询和展示，
 * 不直接保存 Throwable，错误详情通过 AsyncError 中的可序列化字段表达。
 * </p>
 */
public class TaskExecutionSnapshot {

    /** 任务唯一 ID。 */
    private String taskId;

    /** 线程池名称。 */
    private String executorName;

    /** 任务名称。 */
    private String taskName;

    /** 业务标识，例如 orderId/userId/batchNo。 */
    private String bizKey;

    /** 任务描述。 */
    private String description;

    /** 任务标签。 */
    private Map<String, String> tags = new LinkedHashMap<>();

    /** 任务当前状态或最终状态。 */
    private AsyncTaskStatus status;

    /** 结构化错误详情。 */
    private AsyncError error = AsyncError.none();

    /** 任务提交时间戳，毫秒。 */
    private long submitTimeMillis;

    /** 任务开始执行时间戳，毫秒。 */
    private long startTimeMillis;

    /** 任务结束时间戳，毫秒。 */
    private long endTimeMillis;

    /** 排队耗时，毫秒。 */
    private long queueCostMillis;

    /** 执行耗时，毫秒。 */
    private long runCostMillis;

    /** 总耗时，毫秒。 */
    private long totalCostMillis;

    /**
     * 兼容旧展示字段：错误类型。
     *
     * <p>新逻辑优先使用 error.exception.errorClass/rootErrorClass。</p>
     */
    private String errorType;

    /**
     * 兼容旧展示字段：错误消息。
     *
     * <p>新逻辑优先使用 error.exception.errorMessage/rootErrorMessage。</p>
     */
    private String errorMessage;

    public static TaskExecutionSnapshot from(TaskExecutionEvent event) {
        TaskExecutionSnapshot snapshot = new TaskExecutionSnapshot();
        if (event == null) {
            return snapshot;
        }
        snapshot.setTaskId(event.getTaskId());
        snapshot.setExecutorName(event.getExecutorName());
        snapshot.setTaskName(event.getTaskName());
        snapshot.setBizKey(event.getBizKey());
        snapshot.setDescription(event.getDescription());
        snapshot.setTags(event.getTags());
        snapshot.setStatus(event.getStatus());
        snapshot.setError(event.getError());
        snapshot.setSubmitTimeMillis(event.getSubmitTimeMillis());
        snapshot.setStartTimeMillis(event.getStartTimeMillis());
        snapshot.setEndTimeMillis(event.getEndTimeMillis());
        snapshot.setQueueCostMillis(event.getQueueCostMillis());
        snapshot.setRunCostMillis(event.getRunCostMillis());
        snapshot.setTotalCostMillis(event.getTotalCostMillis());
        if (event.getError() != null && event.getError().getException() != null) {
            snapshot.setErrorType(event.getError().getException().getRootErrorClass());
            snapshot.setErrorMessage(event.getError().getException().getRootErrorMessage());
        }
        return snapshot;
    }

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }
    public String getExecutorName() { return executorName; }
    public void setExecutorName(String executorName) { this.executorName = executorName; }
    public String getTaskName() { return taskName; }
    public void setTaskName(String taskName) { this.taskName = taskName; }
    public String getBizKey() { return bizKey; }
    public void setBizKey(String bizKey) { this.bizKey = bizKey; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Map<String, String> getTags() { return tags == null ? Collections.emptyMap() : Collections.unmodifiableMap(tags); }
    public void setTags(Map<String, String> tags) { this.tags = tags == null ? new LinkedHashMap<>() : new LinkedHashMap<>(tags); }
    public AsyncTaskStatus getStatus() { return status; }
    public void setStatus(AsyncTaskStatus status) { this.status = status; }
    public AsyncError getError() { return error; }
    public void setError(AsyncError error) { this.error = error == null ? AsyncError.none() : error.copy(); }
    public long getSubmitTimeMillis() { return submitTimeMillis; }
    public void setSubmitTimeMillis(long submitTimeMillis) { this.submitTimeMillis = submitTimeMillis; }
    public long getStartTimeMillis() { return startTimeMillis; }
    public void setStartTimeMillis(long startTimeMillis) { this.startTimeMillis = startTimeMillis; }
    public long getEndTimeMillis() { return endTimeMillis; }
    public void setEndTimeMillis(long endTimeMillis) { this.endTimeMillis = endTimeMillis; }
    public long getQueueCostMillis() { return queueCostMillis; }
    public void setQueueCostMillis(long queueCostMillis) { this.queueCostMillis = queueCostMillis; }
    public long getRunCostMillis() { return runCostMillis; }
    public void setRunCostMillis(long runCostMillis) { this.runCostMillis = runCostMillis; }
    public long getTotalCostMillis() { return totalCostMillis; }
    public void setTotalCostMillis(long totalCostMillis) { this.totalCostMillis = totalCostMillis; }
    public String getErrorType() { return errorType; }
    public void setErrorType(String errorType) { this.errorType = errorType; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
