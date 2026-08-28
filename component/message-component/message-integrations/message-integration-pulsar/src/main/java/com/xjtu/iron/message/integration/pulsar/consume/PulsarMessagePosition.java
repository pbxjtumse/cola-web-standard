package com.xjtu.iron.message.integration.pulsar.consume;

import com.xjtu.iron.message.spi.ProviderMessagePosition;

import java.util.Map;

/** Pulsar 消息位置。 */
public final class PulsarMessagePosition implements ProviderMessagePosition {
    private final String messageId;

    public PulsarMessagePosition(String messageId) {
        this.messageId = messageId;
    }

    @Override
    public Map<String, String> attributes() {
        return Map.of("pulsar.messageId", messageId);
    }
}
