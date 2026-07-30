package com.xjtu.iron.retry.api;

/**
 * 重试执行生命周期事件类型。
 */
public enum RetryEventType {
    EXECUTION_STARTED,
    ATTEMPT_STARTED,
    ATTEMPT_COMPLETED,
    RETRY_SCHEDULED,
    SUCCEEDED,
    EXHAUSTED,
    NOT_RETRYABLE,
    TIMED_OUT,
    INTERRUPTED,
    ABORTED
}
