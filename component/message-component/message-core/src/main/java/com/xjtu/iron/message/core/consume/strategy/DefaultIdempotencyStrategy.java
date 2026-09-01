package com.xjtu.iron.message.core.consume.strategy;

import com.xjtu.iron.message.api.consume.context.ConsumeContext;
import com.xjtu.iron.message.api.consume.decision.ConsumeDecision;
import com.xjtu.iron.message.api.consume.definition.MessageIdempotencyOptions;
import com.xjtu.iron.message.api.model.MessageEnvelope;
import com.xjtu.iron.message.core.consume.ConsumeInvocation;
import com.xjtu.iron.message.core.consume.idempotency.MessageIdempotencyExecutor;

import java.util.Objects;

public final class DefaultIdempotencyStrategy implements IdempotencyStrategy {

    private final MessageIdempotencyExecutor executor;

    public DefaultIdempotencyStrategy(MessageIdempotencyExecutor executor) {
        this.executor = Objects.requireNonNull(executor, "executor must not be null");
    }

    @Override
    public ConsumeDecision execute(
            MessageEnvelope<?> message,
            ConsumeContext context,
            ConsumeInvocation invocation) {
        return executor.execute(
                message,
                context,
                MessageIdempotencyOptions.messageId(),
                invocation);
    }
}
