package com.xjtu.iron.message.core.consume.strategy;

import com.xjtu.iron.message.api.consume.context.ConsumeContext;
import com.xjtu.iron.message.api.consume.decision.ConsumeDecision;
import com.xjtu.iron.message.api.model.MessageEnvelope;
import com.xjtu.iron.message.core.consume.ConsumeInvocation;

public interface IdempotencyStrategy {

    ConsumeDecision execute(
            MessageEnvelope<?> message,
            ConsumeContext context,
            ConsumeInvocation invocation);
}
