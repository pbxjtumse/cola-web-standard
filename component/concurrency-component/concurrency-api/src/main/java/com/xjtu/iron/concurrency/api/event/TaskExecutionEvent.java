package com.xjtu.iron.concurrency.api.event;

import com.xjtu.iron.concurrency.api.enums.task.AsyncTaskStatus;
import com.xjtu.iron.concurrency.api.error.AsyncError;

import java.time.Instant;
import com.xjtu.iron.concurrency.api.task.TaskMetadata;
import com.xjtu.iron.concurrency.api.task.TaskResultMode;
import com.xjtu.iron.concurrency.api.task.TaskTimingSnapshot;


/**
 * 任务执行事件。
 *
 * <p>
 * 用于向任务监听器、任务状态注册表、指标记录器传递任务执行过程中的状态、耗时、错误和上下文信息。
 * </p>
 */

public final class TaskExecutionEvent {

    /**
     * 任务基础元数据。
     */
    private final TaskMetadata task;

    /**
     * 本次事件对应的任务状态。
     */
    private final AsyncTaskStatus status;

    /**
     * 任务结果模式。
     */
    private final TaskResultMode resultMode;

    /**
     * 截止本次事件发生时的时间和耗时快照。
     */
    private final TaskTimingSnapshot timing;

    /**
     * 结构化错误信息。
     *
     * <p>
     * 正常事件为 AsyncError.none()。
     * </p>
     */
    private final AsyncError error;

    /**
     * 本次事件的人类可读说明。
     */
    private final String message;

    /**
     * 事件发生时间。
     */
    private final Instant occurredAt;

    public TaskExecutionEvent(
            TaskMetadata task,
            AsyncTaskStatus status,
            TaskResultMode resultMode,
            TaskTimingSnapshot timing,
            AsyncError error,
            String message,
            Instant occurredAt
    ) {
        this.task = task;
        this.status = status;
        this.resultMode = resultMode;
        this.timing = timing == null
                ? TaskTimingSnapshot.empty()
                : timing;
        this.error = error == null
                ? AsyncError.none()
                : error.copy();
        this.message = message;
        this.occurredAt = occurredAt == null
                ? Instant.now()
                : occurredAt;
    }

    public TaskExecutionEvent copy() {
        return new TaskExecutionEvent(
                task,
                status,
                resultMode,
                timing,
                error,
                message,
                occurredAt
        );
    }

    public TaskMetadata getTask() {
        return task;
    }

    public AsyncTaskStatus getStatus() {
        return status;
    }

    public TaskResultMode getResultMode() {
        return resultMode;
    }

    public TaskTimingSnapshot getTiming() {
        return timing;
    }

    public AsyncError getError() {
        return error;
    }

    public String getMessage() {
        return message;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}