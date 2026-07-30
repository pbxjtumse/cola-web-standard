package com.xjtu.iron.retry.api.exception;

import com.xjtu.iron.retry.api.RetryResult;

import java.util.Objects;

/**
 * 调用方选择抛异常风格时使用的统一重试执行异常。
 */
public final class RetryExecutionException extends RuntimeException {

    private final RetryResult<?> retryResult;

    public RetryExecutionException(RetryResult<?> retryResult) {
        super(buildMessage(retryResult), retryResult == null ? null : retryResult.getFailure());
        this.retryResult = Objects.requireNonNull(retryResult, "retryResult must not be null");
    }

    public RetryResult<?> getRetryResult() {
        return retryResult;
    }

    private static String buildMessage(RetryResult<?> retryResult) {
        if (retryResult == null) {
            return "Retry execution failed";
        }
        return "Retry execution failed: operation=" + retryResult.getOperationName()
                + ", policy=" + retryResult.getPolicyName()
                + ", status=" + retryResult.getStatus()
                + ", attempts=" + retryResult.getAttempts();
    }
}
