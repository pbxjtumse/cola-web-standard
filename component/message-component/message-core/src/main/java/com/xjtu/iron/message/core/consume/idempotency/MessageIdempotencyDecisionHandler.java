package com.xjtu.iron.message.core.consume.idempotency;

import com.xjtu.iron.message.api.consume.decision.ConsumeDecision;
import com.xjtu.iron.message.api.consume.definition.MessageIdempotencyFailurePolicy;
import com.xjtu.iron.message.core.consume.ConsumeInvocation;
import com.xjtu.iron.message.core.consume.transaction.MessageConsumeTransactionContext;
import com.xjtu.iron.message.core.consume.transaction.MessageConsumeTransactionExecutor;

import java.util.Objects;

/**
 * 根据 acquire 结果决定后续幂等行为。
 */
public final class MessageIdempotencyDecisionHandler {

    private final MessageIdempotencyStateManager stateManager;
    private final MessageConsumeTransactionExecutor transactionExecutor;

    public MessageIdempotencyDecisionHandler(
            MessageIdempotencyStateManager stateManager,
            MessageConsumeTransactionExecutor transactionExecutor) {
        this.stateManager = Objects.requireNonNull(stateManager);
        this.transactionExecutor = Objects.requireNonNull(transactionExecutor);
    }

    public ConsumeDecision handle(
            IdempotentAcquireResult result,
            MessageIdempotencyContext context,
            ConsumeInvocation invocation) {
        return switch (result.status()) {
            case ACQUIRED -> executeAcquired(context, invocation);
            case DUPLICATE_SUCCESS, DUPLICATE_DISCARDED -> ConsumeDecision.ACK;
            case PROCESSING, STORAGE_ERROR -> ConsumeDecision.RETRY;
            case REJECTED -> rejected(context.options().failurePolicy());
        };
    }

    private ConsumeDecision executeAcquired(
            MessageIdempotencyContext context,
            ConsumeInvocation invocation) {
        try {
            return transactionExecutor.execute(
                    new MessageConsumeTransactionContext(context.consumeContext(), context),
                    () -> {
                        ConsumeDecision decision = invocation.invoke();
                        if (decision == ConsumeDecision.ACK) {
                            stateManager.markSuccess(context);
                            return decision;
                        }
                        if (decision == ConsumeDecision.DISCARD) {
                            stateManager.markDiscarded(context);
                            return decision;
                        }
                        throw new RuntimeException("consume requested retry");
                    });
        } catch (RuntimeException exception) {
            try {
                stateManager.markFailed(context, exception);
            } catch (RuntimeException ignored) {
                // best effort
            }
            return ConsumeDecision.RETRY;
        }
    }

    private ConsumeDecision rejected(MessageIdempotencyFailurePolicy policy) {
        return policy == MessageIdempotencyFailurePolicy.DISCARD
                ? ConsumeDecision.DISCARD
                : policy == MessageIdempotencyFailurePolicy.DEAD_LETTER
                ? ConsumeDecision.DEAD_LETTER
                : ConsumeDecision.RETRY;
    }
}
