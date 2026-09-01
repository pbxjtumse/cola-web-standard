package com.xjtu.iron.message.core.consume;

import com.xjtu.iron.message.api.consume.decision.ConsumeDecision;

@FunctionalInterface
public interface ConsumeInvocation {

    ConsumeDecision invoke();
}
