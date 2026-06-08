package com.xjtu.iron.concurrency.api.execution;

import com.xjtu.iron.concurrency.api.enums.AsyncTaskStatus;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 任务状态快照。
 */
public class TaskExecutionSnapshot {

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
    private String errorType;
    private String errorMessage;

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
