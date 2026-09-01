package com.xjtu.iron.message.core.consume.strategy;

import com.xjtu.iron.message.api.consume.context.ConsumeContext;
import com.xjtu.iron.message.api.consume.decision.ConsumeDecision;
import com.xjtu.iron.message.api.model.MessageEnvelope;
import com.xjtu.iron.message.core.consume.ConsumeInvocation;

public final class NoopIdempotencyStrategy implements IdempotencyStrategy {

    @Override
    public ConsumeDecision execute(
            MessageEnvelope<?> message,
            ConsumeContext context,
            ConsumeInvocation invocation) {
        return invocation.invoke();
    }
}
