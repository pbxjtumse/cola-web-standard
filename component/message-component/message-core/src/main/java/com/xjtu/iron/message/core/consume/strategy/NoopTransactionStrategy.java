package com.xjtu.iron.message.core.consume.strategy;

import com.xjtu.iron.message.api.consume.decision.ConsumeDecision;
import com.xjtu.iron.message.core.consume.ConsumeInvocation;

public final class NoopTransactionStrategy implements TransactionStrategy {

    @Override
    public ConsumeDecision execute(ConsumeInvocation invocation) {
        return invocation.invoke();
    }
}
