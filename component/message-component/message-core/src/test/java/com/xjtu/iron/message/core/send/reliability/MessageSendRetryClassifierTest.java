package com.xjtu.iron.message.core.send.reliability;

import com.xjtu.iron.message.api.publish.SendFailureType;
import com.xjtu.iron.message.api.publish.SendStatus;
import com.xjtu.iron.message.spi.ProviderSendResult;
import com.xjtu.iron.retry.api.execution.RetryAttempt;
import com.xjtu.iron.retry.api.policy.RetryDecision;
import com.xjtu.iron.retry.api.policy.RetryDecisionType;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 验证消息发送结果到 retry-component 决策的核心映射。
 */
class MessageSendRetryClassifierTest {

    @Test
    void confirmedResultShouldFinishSuccessfully() {
        MessageSendRetryClassifier classifier = new MessageSendRetryClassifier(false);
        RetryDecision decision = classifier.classify(attempt(ProviderSendResult.confirmed("provider-1"), 1));
        assertEquals(RetryDecisionType.SUCCESS, decision.getType());
    }

    @Test
    void networkFailureShouldRetry() {
        MessageSendRetryClassifier classifier = new MessageSendRetryClassifier(false);
        ProviderSendResult result = ProviderSendResult.failed(
                SendStatus.FAILED,
                SendFailureType.NETWORK_ERROR,
                "temporary network failure");
        RetryDecision decision = classifier.classify(attempt(result, 1));
        assertEquals(RetryDecisionType.RETRY, decision.getType());
    }

    @Test
    void unknownResultShouldStopByDefault() {
        MessageSendRetryClassifier classifier = new MessageSendRetryClassifier(false);
        ProviderSendResult result = ProviderSendResult.failed(
                SendStatus.UNKNOWN,
                SendFailureType.TIMEOUT,
                "confirm timeout");
        RetryDecision decision = classifier.classify(attempt(result, 1));
        assertEquals(RetryDecisionType.STOP, decision.getType());
    }

    @Test
    void unknownResultCanRetryWhenExplicitlyEnabled() {
        MessageSendRetryClassifier classifier = new MessageSendRetryClassifier(true);
        ProviderSendResult result = ProviderSendResult.failed(
                SendStatus.UNKNOWN,
                SendFailureType.TIMEOUT,
                "confirm timeout");
        RetryDecision decision = classifier.classify(attempt(result, 1));
        assertEquals(RetryDecisionType.RETRY, decision.getType());
    }

    @Test
    void rejectedResultShouldStop() {
        MessageSendRetryClassifier classifier = new MessageSendRetryClassifier(false);
        ProviderSendResult result = ProviderSendResult.failed(
                SendStatus.REJECTED,
                SendFailureType.ROUTING_ERROR,
                "invalid topic");
        RetryDecision decision = classifier.classify(attempt(result, 1));
        assertEquals(RetryDecisionType.STOP, decision.getType());
    }

    private static RetryAttempt<ProviderSendResult> attempt(ProviderSendResult result, int attemptNumber) {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        return new RetryAttempt<>(
                "retry-test",
                "message-send",
                "message-send",
                attemptNumber,
                now,
                now,
                now,
                Duration.ZERO,
                Duration.ZERO,
                Duration.ofSeconds(10),
                result,
                null,
                Map.of());
    }
}
