package com.xjtu.iron.message.integration.rocketmq;

/** RocketMQ Provider 诊断元数据键。 */
public final class RocketMqMetadataKeys {

    public static final String MESSAGE_ID = "rocketmq.message-id";
    public static final String DELIVERY_ATTEMPT = "rocketmq.delivery-attempt";
    public static final String BORN_HOST = "rocketmq.born-host";
    public static final String BORN_TIMESTAMP = "rocketmq.born-timestamp";

    private RocketMqMetadataKeys() {
    }
}
