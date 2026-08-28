package com.xjtu.iron.message.integration.pulsar.config;

/** Pulsar Provider 诊断元数据键。 */
public final class PulsarMetadataKeys {

    public static final String TOPIC = "pulsar.topic";
    public static final String MESSAGE_ID = "pulsar.message-id";
    public static final String REDELIVERY_COUNT = "pulsar.redelivery-count";
    public static final String PUBLISH_TIME = "pulsar.publish-time";
    public static final String EVENT_TIME = "pulsar.event-time";

    private PulsarMetadataKeys() {
    }
}
