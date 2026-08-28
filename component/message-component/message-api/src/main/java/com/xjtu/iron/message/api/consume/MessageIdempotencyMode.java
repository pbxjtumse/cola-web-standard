package com.xjtu.iron.message.api.consume;

/**
 * 消费幂等 key 生成模式。
 */
public enum MessageIdempotencyMode {
    /** 不启用消费幂等。 */
    NONE,

    /** 基于 messageId 做幂等，防止同一条消息重复投递。 */
    MESSAGE_ID,

    /** 基于业务 key 做幂等，防止不同 messageId 表达同一个业务动作。 */
    BUSINESS_KEY,

    /** 由业务自定义 resolver 完全决定 key。 */
    CUSTOM
}
