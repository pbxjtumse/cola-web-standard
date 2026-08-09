package com.xjtu.iron.message.core.send.reliability;

import com.xjtu.iron.message.api.SendFailureType;
import com.xjtu.iron.message.api.SendStatus;
import com.xjtu.iron.message.spi.ProviderSendResult;
import com.xjtu.iron.retry.api.execution.RetryAttempt;
import com.xjtu.iron.retry.api.policy.RetryClassifier;
import com.xjtu.iron.retry.api.policy.RetryDecision;
import com.xjtu.iron.retry.api.policy.RetryFailureCategory;

import java.util.Objects;

/**
 * 消息发送专用重试分类器。
 *
 * <p>
 * retry-component 不理解 MQ 语义。
 * 这里由 message-component 把 ProviderSendResult 转换为 RetryDecision。
 * </p>
 */
public final class MessageSendRetryClassifier implements RetryClassifier {

    /** 发送结果 UNKNOWN 时是否允许继续重试。 */
    private final boolean retryWhenUnknown;

    public MessageSendRetryClassifier(boolean retryWhenUnknown) {
        this.retryWhenUnknown = retryWhenUnknown;
    }

    @Override
    public RetryDecision classify(RetryAttempt<?> attempt) {
        Objects.requireNonNull(attempt, "attempt must not be null");
        if (attempt.hasFailure()) {
            return classifyThrowable(attempt.getFailure());
        }
        Object result = attempt.getResult();
        if (!(result instanceof ProviderSendResult providerResult)) {
            return RetryDecision.abort(
                    "message send operation returned non ProviderSendResult",
                    "MESSAGE_SEND_INVALID_RESULT");
        }
        return classifyProviderResult(providerResult);
    }

    private RetryDecision classifyProviderResult(ProviderSendResult result) {
        if (result.status() == SendStatus.CONFIRMED) {
            return RetryDecision.success("message send confirmed by provider");
        }
        if (result.status() == SendStatus.REJECTED) {
            return RetryDecision.stop(
                    "message send rejected by provider",
                    failureCode(result),
                    RetryFailureCategory.NON_RETRYABLE);
        }
        if (result.status() == SendStatus.UNKNOWN) {
            if (retryWhenUnknown) {
                return RetryDecision.retry(
                        "message send outcome is unknown but retryWhenUnknown is enabled",
                        failureCode(result),
                        RetryFailureCategory.UNKNOWN);
            }
            return RetryDecision.stop(
                    "message send outcome is unknown, stop to avoid duplicate message",
                    failureCode(result),
                    RetryFailureCategory.UNKNOWN);
        }
        if (isRetryableFailure(result.failureType())) {
            return RetryDecision.retry(
                    "message send failed with retryable failure type",
                    failureCode(result),
                    retryCategory(result.failureType()));
        }
        return RetryDecision.stop(
                "message send failed with non retryable failure type",
                failureCode(result),
                RetryFailureCategory.NON_RETRYABLE);
    }

    private RetryDecision classifyThrowable(Throwable throwable) {
        if (throwable instanceof InterruptedException) {
            return RetryDecision.abort("message send interrupted", "MESSAGE_SEND_INTERRUPTED");
        }
        return RetryDecision.stop(
                throwable == null ? "unknown message send exception" : throwable.getMessage(),
                "MESSAGE_SEND_EXCEPTION",
                RetryFailureCategory.UNKNOWN);
    }

    private static boolean isRetryableFailure(SendFailureType failureType) {
        return failureType == SendFailureType.NETWORK_ERROR
                || failureType == SendFailureType.CLIENT_ERROR;
    }

    private static RetryFailureCategory retryCategory(SendFailureType failureType) {
        if (failureType == SendFailureType.NETWORK_ERROR) {
            return RetryFailureCategory.DEPENDENCY_UNAVAILABLE;
        }
        return RetryFailureCategory.TRANSIENT;
    }

    private static String failureCode(ProviderSendResult result) {
        if (result.failureType() == null) {
            return "MESSAGE_SEND_UNKNOWN_FAILURE";
        }
        return "MESSAGE_SEND_" + result.failureType().name();
    }
}
