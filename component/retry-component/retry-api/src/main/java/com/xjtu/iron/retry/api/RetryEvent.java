package com.xjtu.iron.retry.api;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * 重试生命周期事件不可变快照。
 *
 * <p>事件不会携带完整业务请求体和返回值，避免敏感信息泄漏。</p>
 */
public final class RetryEvent {

    private final RetryEventType eventType;
    private final String retryId;
    private final String operationName;
    private final String policyName;
    private final int attempt;
    private final int maxAttempts;
    private final Instant occurredAt;
    private final Duration elapsedTime;
    private final Duration attemptDuration;
    private final Duration nextDelay;
    private final RetryStatus finalStatus;
    private final Throwable failure;

    public RetryEvent(
            RetryEventType eventType,
            String retryId,
            String operationName,
            String policyName,
            int attempt,
            int maxAttempts,
            Instant occurredAt,
            Duration elapsedTime,
            Duration attemptDuration,
            Duration nextDelay,
            RetryStatus finalStatus,
            Throwable failure) {
        this.eventType = Objects.requireNonNull(eventType, "eventType must not be null");
        this.retryId = Objects.requireNonNull(retryId, "retryId must not be null");
        this.operationName = Objects.requireNonNull(operationName, "operationName must not be null");
        this.policyName = Objects.requireNonNull(policyName, "policyName must not be null");
        this.attempt = attempt;
        this.maxAttempts = maxAttempts;
        this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        this.elapsedTime = elapsedTime == null ? Duration.ZERO : elapsedTime;
        this.attemptDuration = attemptDuration == null ? Duration.ZERO : attemptDuration;
        this.nextDelay = nextDelay == null ? Duration.ZERO : nextDelay;
        this.finalStatus = finalStatus;
        this.failure = failure;
    }

    public RetryEventType getEventType() {
        return eventType;
    }

    public String getRetryId() {
        return retryId;
    }

    public String getOperationName() {
        return operationName;
    }

    public String getPolicyName() {
        return policyName;
    }

    public int getAttempt() {
        return attempt;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public Duration getElapsedTime() {
        return elapsedTime;
    }

    public Duration getAttemptDuration() {
        return attemptDuration;
    }

    public Duration getNextDelay() {
        return nextDelay;
    }

    public RetryStatus getFinalStatus() {
        return finalStatus;
    }

    public Throwable getFailure() {
        return failure;
    }
}
