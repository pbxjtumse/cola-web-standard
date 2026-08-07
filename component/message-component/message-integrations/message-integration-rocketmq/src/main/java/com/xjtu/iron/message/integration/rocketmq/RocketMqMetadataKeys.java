package com.xjtu.iron.message.integration.rocketmq;

/**
 * RocketMQ Provider 写入公共结果元数据的 key。
 */
public final class RocketMqMetadataKeys {

    /** RocketMQ Topic。 */
    public static final String TOPIC = "rocketmq.topic";

    /** Broker 名称。 */
    public static final String BROKER_NAME = "rocketmq.brokerName";

    /** 队列 ID。 */
    public static final String QUEUE_ID = "rocketmq.queueId";

    /** 队列 offset。 */
    public static final String QUEUE_OFFSET = "rocketmq.queueOffset";

    /** 发送状态。 */
    public static final String SEND_STATUS = "rocketmq.sendStatus";

    /** 发送目标 MessageQueue。 */
    public static final String MESSAGE_QUEUE = "rocketmq.messageQueue";

    /** 重消费次数。 */
    public static final String RECONSUME_TIMES = "rocketmq.reconsumeTimes";

    private RocketMqMetadataKeys() {
    }
}
