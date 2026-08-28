package com.xjtu.iron.message.integration.pulsar.consume;

import com.xjtu.iron.message.api.consume.ConsumeDecision;

/** Pulsar 消费决策映射说明类。 */
public final class PulsarConsumeDecisionMapper {
    public PulsarConsumeAction map(ConsumeDecision decision) {
        if (decision == ConsumeDecision.ACK || decision == ConsumeDecision.DISCARD) {
            return PulsarConsumeAction.ACKNOWLEDGE;
        }
        return PulsarConsumeAction.NEGATIVE_ACKNOWLEDGE;
    }

    public enum PulsarConsumeAction {
        ACKNOWLEDGE,
        NEGATIVE_ACKNOWLEDGE
    }
}
