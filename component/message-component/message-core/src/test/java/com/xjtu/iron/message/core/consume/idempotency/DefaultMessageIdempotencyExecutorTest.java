package com.xjtu.iron.message.core.consume.idempotency;

import com.xjtu.iron.message.api.consume.context.ConsumeContext;
import com.xjtu.iron.message.api.consume.decision.ConsumeDecision;
import com.xjtu.iron.message.api.consume.definition.MessageIdempotencyOptions;
import com.xjtu.iron.message.api.model.MessageEnvelope;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DefaultMessageIdempotencyExecutorTest {
    @Test
    void shouldSkipHandlerWhenDuplicateSuccess() {
        SpyOperations operations = new SpyOperations(IdempotentAcquireStatus.DUPLICATE_SUCCESS);
        DefaultMessageIdempotencyExecutor executor = new DefaultMessageIdempotencyExecutor(operations);
        AtomicInteger handlerCalls = new AtomicInteger();
        ConsumeDecision decision = executor.execute(message(), context(), MessageIdempotencyOptions.messageId(), () -> {
            handlerCalls.incrementAndGet();
            return ConsumeDecision.ACK;
        });
        assertEquals(ConsumeDecision.ACK, decision);
        assertEquals(0, handlerCalls.get());
        assertEquals(1, operations.acquireCalls.get());
    }

    @Test
    void shouldMarkSuccessWhenAcquiredAndHandlerAck() {
        SpyOperations operations = new SpyOperations(IdempotentAcquireStatus.ACQUIRED);
        DefaultMessageIdempotencyExecutor executor = new DefaultMessageIdempotencyExecutor(operations);
        ConsumeDecision decision = executor.execute(message(), context(), MessageIdempotencyOptions.messageId(), () -> ConsumeDecision.ACK);
        assertEquals(ConsumeDecision.ACK, decision);
        assertEquals(1, operations.markSuccessCalls.get());
        assertEquals(0, operations.markFailedCalls.get());
    }

    @Test
    void shouldReturnRetryWhenStorageError() {
        SpyOperations operations = new SpyOperations(IdempotentAcquireStatus.STORAGE_ERROR);
        DefaultMessageIdempotencyExecutor executor = new DefaultMessageIdempotencyExecutor(operations);
        ConsumeDecision decision = executor.execute(message(), context(), MessageIdempotencyOptions.messageId(), () -> ConsumeDecision.ACK);
        assertEquals(ConsumeDecision.RETRY, decision);
        assertEquals(0, operations.markSuccessCalls.get());
    }

    private static MessageEnvelope<String> message() {
        return MessageEnvelope.builder("OrderPaid", "payload")
                .messageId("msg-001")
                .messageKey("order-10001")
                .build();
    }

    private static ConsumeContext context() {
        return new ConsumeContext("kafka", "order-topic", "group", "order-topic-0@6", 1, Instant.now(), Map.of());
    }

    private static final class SpyOperations implements MessageIdempotentOperations {
        private final IdempotentAcquireStatus status;
        private final AtomicInteger acquireCalls = new AtomicInteger();
        private final AtomicInteger markSuccessCalls = new AtomicInteger();
        private final AtomicInteger markFailedCalls = new AtomicInteger();

        private SpyOperations(IdempotentAcquireStatus status) {
            this.status = status;
        }

        @Override
        public IdempotentAcquireResult acquire(MessageIdempotencyContext context) {
            acquireCalls.incrementAndGet();
            return IdempotentAcquireResult.of(status);
        }

        @Override
        public void markSuccess(MessageIdempotencyContext context, String resultCode, String resultSnapshot) {
            markSuccessCalls.incrementAndGet();
        }

        @Override
        public void markFailed(MessageIdempotencyContext context, String errorCode, String errorMessage, String errorType) {
            markFailedCalls.incrementAndGet();
        }

        @Override
        public void markDiscarded(MessageIdempotencyContext context, String resultCode, String resultSnapshot) {
        }
    }
}
