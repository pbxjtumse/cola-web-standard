package com.xjtu.iron.message.integration.rocketmq.consume;

import com.xjtu.iron.message.spi.ProviderMessagePosition;

import java.util.Map;

/** RocketMQ 消息位置。 */
public final class RocketMqMessagePosition implements ProviderMessagePosition {
    private final int queueId;
    private final long queueOffset;

    public RocketMqMessagePosition(int queueId, long queueOffset) {
        this.queueId = queueId;
        this.queueOffset = queueOffset;
    }

    @Override
    public Map<String, String> attributes() {
        return Map.of("rocketmq.queueId", Integer.toString(queueId), "rocketmq.queueOffset", Long.toString(queueOffset));
    }
}
