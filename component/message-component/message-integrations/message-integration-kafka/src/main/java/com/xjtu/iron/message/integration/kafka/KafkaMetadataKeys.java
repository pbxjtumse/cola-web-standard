package com.xjtu.iron.message.integration.kafka;

/** Kafka Provider 诊断元数据键，避免在发送和消费流程中散落字符串字面量。 */
public final class KafkaMetadataKeys {

    public static final String TOPIC = "kafka.topic";
    public static final String PARTITION = "kafka.partition";
    public static final String OFFSET = "kafka.offset";
    public static final String TIMESTAMP = "kafka.timestamp";

    private KafkaMetadataKeys() {
    }
}
