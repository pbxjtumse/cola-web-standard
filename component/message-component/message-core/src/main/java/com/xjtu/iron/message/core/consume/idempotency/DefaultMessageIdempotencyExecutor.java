package com.xjtu.iron.message.core.consume.idempotency;

import com.xjtu.iron.message.api.consume.context.ConsumeContext;
import com.xjtu.iron.message.api.consume.decision.ConsumeDecision;
import com.xjtu.iron.message.api.consume.definition.MessageIdempotencyFailurePolicy;
import com.xjtu.iron.message.api.consume.definition.MessageIdempotencyOptions;
import com.xjtu.iron.message.api.model.MessageEnvelope;
import com.xjtu.iron.message.core.consume.ConsumeInvocation;
import com.xjtu.iron.message.core.consume.transaction.MessageConsumeTransactionContext;
import com.xjtu.iron.message.core.consume.transaction.MessageConsumeTransactionExecutor;
import com.xjtu.iron.message.core.consume.transaction.NoopMessageConsumeTransactionExecutor;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.zip.CRC32;

/**
 * 基于 idempotent-component 协作接口的消费幂等执行器。
 *
 * <p>它不直接写数据库，也不直接指定真实表名。真实存储由 MessageIdempotentOperations 背后的幂等组件决定。</p>
 */
public final class DefaultMessageIdempotencyExecutor implements MessageIdempotencyExecutor {
    private final MessageIdempotentOperations operations;
    private final MessageIdempotencySceneResolver sceneResolver;
    private final MessageIdempotencyKeyResolver keyResolver;
    private final MessageIdempotencyOwnerTokenGenerator ownerTokenGenerator;
    private final MessageConsumeTransactionExecutor transactionExecutor;
    private final Clock clock;

    public DefaultMessageIdempotencyExecutor(MessageIdempotentOperations operations) {
        this(
                operations,
                new DefaultMessageIdempotencySceneResolver(),
                new DefaultMessageIdempotencyKeyResolver(),
                new MessageIdempotencyOwnerTokenGenerator(),
                new NoopMessageConsumeTransactionExecutor(),
                Clock.systemUTC());
    }

    public DefaultMessageIdempotencyExecutor(
            MessageIdempotentOperations operations,
            MessageIdempotencySceneResolver sceneResolver,
            MessageIdempotencyKeyResolver keyResolver,
            MessageIdempotencyOwnerTokenGenerator ownerTokenGenerator,
            MessageConsumeTransactionExecutor transactionExecutor,
            Clock clock) {
        this.operations = Objects.requireNonNull(operations, "operations must not be null");
        this.sceneResolver = Objects.requireNonNull(sceneResolver, "sceneResolver must not be null");
        this.keyResolver = Objects.requireNonNull(keyResolver, "keyResolver must not be null");
        this.ownerTokenGenerator = Objects.requireNonNull(ownerTokenGenerator, "ownerTokenGenerator must not be null");
        this.transactionExecutor = Objects.requireNonNull(transactionExecutor, "transactionExecutor must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public ConsumeDecision execute(
            MessageEnvelope<?> message,
            ConsumeContext context,
            MessageIdempotencyOptions options,
            ConsumeInvocation invocation) {
        if (options == null || !options.enabled()) {
            return invocation.invoke();
        }
        MessageIdempotencyContext idempotencyContext = buildContext(message, context, options);
        IdempotentAcquireResult acquireResult;
        try {
            acquireResult = operations.acquire(idempotencyContext);
        } catch (RuntimeException exception) {
            return ConsumeDecision.RETRY;
        }
        return switch (acquireResult.status()) {
            case ACQUIRED -> handleAcquired(idempotencyContext, invocation);
            case DUPLICATE_SUCCESS, DUPLICATE_DISCARDED -> ConsumeDecision.ACK;
            case PROCESSING, STORAGE_ERROR -> ConsumeDecision.RETRY;
            case REJECTED -> rejectedDecision(options.failurePolicy(), idempotencyContext);
        };
    }

    private ConsumeDecision handleAcquired(MessageIdempotencyContext context, ConsumeInvocation invocation) {
        try {
            return transactionExecutor.execute(new MessageConsumeTransactionContext(context.consumeContext(), context), () -> {
                ConsumeDecision decision = invocation.invoke();
                if (decision == null || decision == ConsumeDecision.RETRY) {
                    throw new RetryRequestedException("business requested retry");
                }
                if (decision == ConsumeDecision.ACK) {
                    operations.markSuccess(context, "SUCCESS", null);
                    return ConsumeDecision.ACK;
                }
                if (decision == ConsumeDecision.DISCARD) {
                    operations.markDiscarded(context, "DISCARDED", null);
                    return ConsumeDecision.DISCARD;
                }
                throw new RetryRequestedException("dead letter is not implemented in v13");
            });
        } catch (RuntimeException exception) {
            try {
                operations.markFailed(context, "CONSUME_FAILED", exception.getMessage(), exception.getClass().getName());
            } catch (RuntimeException ignored) {
                // markFailed 是回滚后的尽力记录，失败也不能 ACK。
            }
            return ConsumeDecision.RETRY;
        }
    }

    private ConsumeDecision rejectedDecision(MessageIdempotencyFailurePolicy policy, MessageIdempotencyContext context) {
        if (policy == MessageIdempotencyFailurePolicy.DISCARD) {
            try {
                operations.markDiscarded(context, "MAX_ATTEMPTS_DISCARDED", null);
            } catch (RuntimeException ignored) {
                return ConsumeDecision.RETRY;
            }
            return ConsumeDecision.DISCARD;
        }
        if (policy == MessageIdempotencyFailurePolicy.DEAD_LETTER) {
            return ConsumeDecision.DEAD_LETTER;
        }
        return ConsumeDecision.RETRY;
    }

    private MessageIdempotencyContext buildContext(
            MessageEnvelope<?> message,
            ConsumeContext context,
            MessageIdempotencyOptions options) {
        String scene = sceneResolver.resolve(message, context, options);
        String key = keyResolver.resolve(message, context, options);
        String ownerToken = ownerTokenGenerator.nextToken();
        long shardKey = positiveCrc32(options.namespace() + ':' + scene + ':' + key);
        Instant now = clock.instant();
        return new MessageIdempotencyContext(
                options.namespace(),
                scene,
                key,
                shardKey,
                ownerToken,
                options.storeName(),
                options.maxAttempts(),
                now.plus(options.processingTimeout()),
                now.plus(options.recordRetention()),
                message,
                context.withIdempotency(scene, key, options.mode()),
                options);
    }

    private static long positiveCrc32(String value) {
        CRC32 crc32 = new CRC32();
        crc32.update(value.getBytes(StandardCharsets.UTF_8));
        return crc32.getValue() & 0x7fffffffL;
    }

    @SuppressWarnings("serial")
    private static final class RetryRequestedException extends RuntimeException {
        private RetryRequestedException(String message) {
            super(message);
        }
    }
}
