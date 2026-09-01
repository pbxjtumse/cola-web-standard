package com.xjtu.iron.message.core.consume.strategy;

import com.xjtu.iron.message.api.consume.decision.ConsumeDecision;
import com.xjtu.iron.message.core.consume.ConsumeInvocation;

public interface TransactionStrategy {

    ConsumeDecision execute(ConsumeInvocation invocation);
}
