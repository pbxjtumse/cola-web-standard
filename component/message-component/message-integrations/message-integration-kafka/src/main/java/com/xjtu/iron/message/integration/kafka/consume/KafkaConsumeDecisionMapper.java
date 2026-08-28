package com.xjtu.iron.message.integration.kafka.consume;

import com.xjtu.iron.message.api.consume.ConsumeDecision;

/**
 * Kafka 消费决策映射说明类。
 */
public final class KafkaConsumeDecisionMapper {
    public KafkaConsumeAction map(ConsumeDecision decision) {
        if (decision == ConsumeDecision.ACK || decision == ConsumeDecision.DISCARD) {
            return KafkaConsumeAction.COMMIT_NEXT_OFFSET;
        }
        return KafkaConsumeAction.RETRY_WITHOUT_COMMIT;
    }

    public enum KafkaConsumeAction {
        COMMIT_NEXT_OFFSET,
        RETRY_WITHOUT_COMMIT
    }
}
