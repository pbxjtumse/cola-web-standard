package com.xjtu.iron.concurrency.api.execution.registry;

import com.xjtu.iron.concurrency.api.enums.task.AsyncTaskStatus;
import com.xjtu.iron.concurrency.api.error.AsyncError;
import com.xjtu.iron.concurrency.api.task.TaskMetadata;
import com.xjtu.iron.concurrency.api.task.TaskResultMode;
import com.xjtu.iron.concurrency.api.task.TaskTimingSnapshot;

public class TaskExecutionSnapshot {

    /**
     * 任务基础信息。
     */
    private TaskMetadata task;

    /**
     * 当前任务状态。
     */
    private AsyncTaskStatus status;

    /**
     * 任务结果模式。
     */
    private TaskResultMode resultMode;

    /**
     * 时间和耗时信息。
     */
    private TaskTimingSnapshot timing = TaskTimingSnapshot.empty();

    /**
     * 结构化错误信息。
     */
    private AsyncError error = AsyncError.none();

    /**
     * 快照最近更新时间。
     */
    private long updatedAtMillis;

    public TaskMetadata getTask() {
        return task;
    }

    public void setTask(TaskMetadata task) {
        this.task = task;
    }

    public AsyncTaskStatus getStatus() {
        return status;
    }

    public void setStatus(AsyncTaskStatus status) {
        this.status = status;
    }

    public TaskResultMode getResultMode() {
        return resultMode;
    }

    public void setResultMode(TaskResultMode resultMode) {
        this.resultMode = resultMode;
    }

    public TaskTimingSnapshot getTiming() {
        return timing;
    }

    public void setTiming(TaskTimingSnapshot timing) {
        this.timing = timing;
    }

    public AsyncError getError() {
        return error;
    }

    public void setError(AsyncError error) {
        this.error = error;
    }

    public long getUpdatedAtMillis() {
        return updatedAtMillis;
    }

    public void setUpdatedAtMillis(long updatedAtMillis) {
        this.updatedAtMillis = updatedAtMillis;
    }
}