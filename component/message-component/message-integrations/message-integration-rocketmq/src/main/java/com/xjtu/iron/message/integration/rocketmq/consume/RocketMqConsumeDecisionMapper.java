package com.xjtu.iron.message.integration.rocketmq.consume;

import com.xjtu.iron.message.api.consume.ConsumeDecision;

/** RocketMQ 4 消费决策映射说明类。 */
public final class RocketMqConsumeDecisionMapper {
    public RocketMqConsumeAction map(ConsumeDecision decision) {
        if (decision == ConsumeDecision.ACK || decision == ConsumeDecision.DISCARD) {
            return RocketMqConsumeAction.CONSUME_SUCCESS;
        }
        return RocketMqConsumeAction.RECONSUME_LATER;
    }

    public enum RocketMqConsumeAction {
        CONSUME_SUCCESS,
        RECONSUME_LATER
    }
}
