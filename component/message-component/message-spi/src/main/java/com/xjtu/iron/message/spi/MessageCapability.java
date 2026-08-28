package com.xjtu.iron.message.spi;

/**
 * 描述 Provider 对公共消息能力的支持情况。
 */
public enum MessageCapability {
    /** 支持普通消息发布。 */
    BASIC_PUBLISH,

    /** 支持普通消息消费。 */
    BASIC_CONSUME,

    /** 支持消费者显式提交或推进 offset。 */
    OFFSET_COMMIT,

    /** 支持手动 ACK。 */
    MANUAL_ACK,

    /** 支持 Negative ACK。 */
    NEGATIVE_ACK,

    /** 通过 listener 返回消费状态表达 ACK/RETRY，例如 RocketMQ 4。 */
    RETURN_CONSUME_STATUS,

    /** 支持消息重新投递。 */
    REDELIVERY,

    /** 支持死信能力。 */
    DEAD_LETTER,

    /** 支持分区或队列内顺序消费。 */
    ORDERED_CONSUME,

    /** 原生回调可能批量投递消息。 */
    BATCH_CONSUME
}
