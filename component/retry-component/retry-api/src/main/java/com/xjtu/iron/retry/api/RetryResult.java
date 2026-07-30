package com.xjtu.iron.retry.api;

import com.xjtu.iron.retry.api.exception.RetryExecutionException;

import java.time.Duration;
import java.util.Objects;

/**
 * 一次逻辑重试执行的统一结果。
 *
 * @param <T> 业务返回值类型
 */
public final class RetryResult<T> {

    private final String retryId;
    private final String operationName;
    private final String policyName;
    private final RetryStatus status;
    private final T value;
    private final Throwable failure;
    private final int attempts;
    private final Duration elapsedTime;

    private RetryResult(
            String retryId,
            String operationName,
            String policyName,
            RetryStatus status,
            T value,
            Throwable failure,
            int attempts,
            Duration elapsedTime) {
        this.retryId = Objects.requireNonNull(retryId, "retryId must not be null");
        this.operationName = Objects.requireNonNull(operationName, "operationName must not be null");
        this.policyName = Objects.requireNonNull(policyName, "policyName must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.value = value;
        this.failure = failure;
        if (attempts < 0) {
            throw new IllegalArgumentException("attempts must not be negative");
        }
        this.attempts = attempts;
        this.elapsedTime = Objects.requireNonNull(elapsedTime, "elapsedTime must not be null");
    }

    public static <T> RetryResult<T> of(
            String retryId,
            String operationName,
            String policyName,
            RetryStatus status,
            T value,
            Throwable failure,
            int attempts,
            Duration elapsedTime) {
        return new RetryResult<>(
                retryId,
                operationName,
                policyName,
                status,
                value,
                failure,
                attempts,
                elapsedTime
        );
    }

    public boolean isSuccess() {
        return status == RetryStatus.SUCCESS;
    }

    public boolean isExhausted() {
        return status == RetryStatus.EXHAUSTED;
    }

    /**
     * 成功时返回业务值，失败时抛出包含完整 RetryResult 的统一异常。
     */
    public T getOrThrow() {
        if (isSuccess()) {
            return value;
        }
        throw new RetryExecutionException(this);
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

    public RetryStatus getStatus() {
        return status;
    }

    public T getValue() {
        return value;
    }

    public Throwable getFailure() {
        return failure;
    }

    public int getAttempts() {
        return attempts;
    }

    public Duration getElapsedTime() {
        return elapsedTime;
    }
}
