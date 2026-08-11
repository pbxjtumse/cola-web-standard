package com.xjtu.iron.idempotent.api;
/** V1 仅保留三个持久状态，是否可重试由 failureRetryable 单独描述。 */
public enum IdempotencyStatus {
    PROCESSING,
    SUCCESS,
    FAILED
}
