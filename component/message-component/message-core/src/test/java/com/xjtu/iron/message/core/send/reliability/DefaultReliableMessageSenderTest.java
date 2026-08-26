package com.xjtu.iron.message.core.send.reliability;

import com.xjtu.iron.message.api.consume.ConsumeDecision;
import com.xjtu.iron.message.api.model.MessageDestination;
import com.xjtu.iron.message.api.model.MessageEnvelope;
import com.xjtu.iron.message.api.publish.SendFailureType;
import com.xjtu.iron.message.api.publish.SendResult;
import com.xjtu.iron.message.api.publish.SendStatus;
import com.xjtu.iron.message.core.send.MessageSendReliabilityOptions;
import com.xjtu.iron.message.core.send.PreparedMessageSend;
import com.xjtu.iron.message.spi.MessageCapability;
import com.xjtu.iron.message.spi.MessageProvider;
import com.xjtu.iron.message.spi.ProviderDestination;
import com.xjtu.iron.message.spi.ProviderInboundMessage;
import com.xjtu.iron.message.spi.ProviderSendRequest;
import com.xjtu.iron.message.spi.ProviderSendResult;
import com.xjtu.iron.message.spi.ProviderSubscription;
import com.xjtu.iron.message.spi.ProviderSubscriptionRequest;
import com.xjtu.iron.retry.api.execution.RetryAttempt;
import com.xjtu.iron.retry.api.execution.RetryContext;
import com.xjtu.iron.retry.api.execution.RetryExecution;
import com.xjtu.iron.retry.api.execution.RetryExecutor;
import com.xjtu.iron.retry.api.execution.RetryResult;
import com.xjtu.iron.retry.api.execution.RetryStatus;
import com.xjtu.iron.retry.api.policy.RetryDecision;
import com.xjtu.iron.retry.api.policy.RetryDecisionType;
import com.xjtu.iron.retry.api.policy.RetryPolicy;
import com.xjtu.iron.retry.api.policy.RetryPolicyRegistry;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 使用 FakeMessageProvider 验证可靠发送核心分支。
 */
class DefaultReliableMessageSenderTest {

    @Test
    void retryableFailureShouldRetryAndReturnConfirmed() {
        FakeMessageProvider provider = new FakeMessageProvider(
                ProviderSendResult.failed(SendStatus.FAILED, SendFailureType.NETWORK_ERROR, "network"),
                ProviderSendResult.confirmed("provider-message-1"));
        SendResult result = sender().send(prepared(provider));
        assertEquals(SendStatus.CONFIRMED, result.status());
        assertEquals("SUCCESS", result.reliabilityInfo().retryStatus());
        assertEquals(2, result.reliabilityInfo().attempts());
    }

    @Test
    void repeatedRetryableFailureShouldReturnRetryExhausted() {
        FakeMessageProvider provider = new FakeMessageProvider(
                ProviderSendResult.failed(SendStatus.FAILED, SendFailureType.NETWORK_ERROR, "network-1"),
                ProviderSendResult.failed(SendStatus.FAILED, SendFailureType.NETWORK_ERROR, "network-2"),
                ProviderSendResult.failed(SendStatus.FAILED, SendFailureType.NETWORK_ERROR, "network-3"));
        SendResult result = sender().send(prepared(provider));
        assertEquals(SendStatus.FAILED, result.status());
        assertEquals(SendFailureType.RETRY_EXHAUSTED, result.failureType());
        assertEquals("EXHAUSTED", result.reliabilityInfo().retryStatus());
        assertEquals(3, result.reliabilityInfo().attempts());
    }

    @Test
    void unknownResultShouldStopWithoutRetry() {
        FakeMessageProvider provider = new FakeMessageProvider(
                ProviderSendResult.failed(SendStatus.UNKNOWN, SendFailureType.TIMEOUT, "timeout"),
                ProviderSendResult.confirmed("should-not-send"));
        SendResult result = sender().send(prepared(provider));
        assertEquals(SendStatus.UNKNOWN, result.status());
        assertEquals(SendFailureType.TIMEOUT, result.failureType());
        assertEquals("NOT_RETRYABLE", result.reliabilityInfo().retryStatus());
        assertEquals(1, result.reliabilityInfo().attempts());
    }

    private static DefaultReliableMessageSender sender() {
        RetryPolicy policy = RetryPolicy.builder("message-send")
                .maxAttempts(3)
                .maxDuration(Duration.ofSeconds(5))
                .classifier(new MessageSendRetryClassifier(false))
                .build();
        InMemoryRetryPolicyRegistry registry = new InMemoryRetryPolicyRegistry(policy);
        return new DefaultReliableMessageSender(
                new InlineRetryExecutor(),
                registry,
                MessageSendReliabilityOptions.defaults(),
                Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC),
                Runnable::run);
    }

    private static PreparedMessageSend prepared(MessageProvider provider) {
        MessageDestination destination = MessageDestination.of("demo", "message").withProviderHint("fake");
        ProviderDestination providerDestination = new ProviderDestination("fake", "fake-topic", Map.of());
        MessageEnvelope<String> message = MessageEnvelope.builder("DemoMessage", "payload")
                .messageId("message-1")
                .messageKey("order-1")
                .build();
        ProviderSendRequest request = new ProviderSendRequest(
                providerDestination,
                "message-1",
                "order-1",
                Map.of(),
                new byte[]{1});
        return new PreparedMessageSend(
                destination,
                message,
                providerDestination,
                provider,
                request,
                Duration.ofMillis(100),
                Instant.parse("2026-01-01T00:00:00Z"));
    }

    private static final class FakeMessageProvider implements MessageProvider {
        private final Queue<ProviderSendResult> results = new ArrayDeque<>();

        private FakeMessageProvider(ProviderSendResult... results) {
            this.results.addAll(java.util.List.of(results));
        }

        @Override
        public String name() {
            return "fake";
        }

        @Override
        public Set<MessageCapability> capabilities() {
            return Set.of(MessageCapability.BASIC_PUBLISH);
        }

        @Override
        public CompletableFuture<ProviderSendResult> send(ProviderSendRequest request) {
            ProviderSendResult result = results.isEmpty()
                    ? ProviderSendResult.failed(SendStatus.FAILED, SendFailureType.CLIENT_ERROR, "no fake result")
                    : results.remove();
            return CompletableFuture.completedFuture(result);
        }

        @Override
        public ProviderSubscription subscribe(ProviderSubscriptionRequest request) {
            return () -> { };
        }

        @Override
        public void close() {
            // fake provider has no resource.
        }
    }

    private static final class InMemoryRetryPolicyRegistry implements RetryPolicyRegistry {
        private final RetryPolicy policy;

        private InMemoryRetryPolicyRegistry(RetryPolicy policy) {
            this.policy = policy;
        }

        @Override
        public void register(RetryPolicy retryPolicy) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void replace(RetryPolicy retryPolicy) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<RetryPolicy> find(String policyName) {
            return policy.getPolicyName().equals(policyName) ? Optional.of(policy) : Optional.empty();
        }

        @Override
        public RetryPolicy getRequired(String policyName) {
            return find(policyName).orElseThrow(() -> new IllegalArgumentException(policyName));
        }

        @Override
        public Collection<String> policyNames() {
            return java.util.List.of(policy.getPolicyName());
        }

        @Override
        public Map<String, RetryPolicy> snapshot() {
            return Map.of(policy.getPolicyName(), policy);
        }
    }

    private static final class InlineRetryExecutor implements RetryExecutor {

        @Override
        public <T> RetryResult<T> execute(RetryExecution<T> execution) {
            String retryId = execution.getRetryId() == null ? "retry-inline" : execution.getRetryId();
            String operation = execution.getOperationName();
            RetryPolicy policy = execution.getRetryPolicy();
            RetryAttempt<T> lastAttempt = null;
            RetryDecision lastDecision = null;
            Instant now = Instant.parse("2026-01-01T00:00:00Z");
            for (int attemptNumber = 1; attemptNumber <= policy.getMaxAttempts(); attemptNumber++) {
                T result;
                Throwable failure = null;
                try {
                    RetryContext context = new RetryContext(
                            retryId,
                            operation,
                            policy.getPolicyName(),
                            attemptNumber,
                            now,
                            Duration.ZERO,
                            policy.getMaxDuration(),
                            lastAttempt,
                            execution.getAttributes(),
                            execution.getCancellationToken());
                    result = execution.getOperation().execute(context);
                } catch (Exception exception) {
                    result = null;
                    failure = exception;
                }
                lastAttempt = new RetryAttempt<>(
                        retryId,
                        operation,
                        policy.getPolicyName(),
                        attemptNumber,
                        now,
                        now,
                        now,
                        Duration.ZERO,
                        Duration.ZERO,
                        policy.getMaxDuration(),
                        result,
                        failure,
                        execution.getAttributes());
                lastDecision = policy.getRetryClassifier().classify(lastAttempt);
                if (lastDecision.getType() == RetryDecisionType.SUCCESS) {
                    return RetryResult.of(retryId, operation, policy.getPolicyName(), RetryStatus.SUCCESS,
                            result, null, attemptNumber, Duration.ZERO, lastAttempt, lastDecision);
                }
                if (lastDecision.getType() == RetryDecisionType.STOP) {
                    return RetryResult.of(retryId, operation, policy.getPolicyName(), RetryStatus.NOT_RETRYABLE,
                            result, failure, attemptNumber, Duration.ZERO, lastAttempt, lastDecision);
                }
                if (lastDecision.getType() == RetryDecisionType.ABORT) {
                    return RetryResult.of(retryId, operation, policy.getPolicyName(), RetryStatus.ABORTED,
                            result, failure, attemptNumber, Duration.ZERO, lastAttempt, lastDecision);
                }
            }
            T lastValue = lastAttempt == null ? null : lastAttempt.getResult();
            Throwable lastFailure = lastAttempt == null ? null : lastAttempt.getFailure();
            return RetryResult.of(retryId, operation, policy.getPolicyName(), RetryStatus.EXHAUSTED,
                    lastValue, lastFailure, policy.getMaxAttempts(), Duration.ZERO, lastAttempt, lastDecision);
        }

        @Override
        public <T> RetryResult<T> execute(String operationName, Map<String, Object> attributes,
                                          com.xjtu.iron.retry.api.execution.RetryOperation<T> operation,
                                          String policyName) {
            throw new UnsupportedOperationException();
        }
    }
}
