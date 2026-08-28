package com.xjtu.iron.message.api.consume.definition;

/**
 * 幂等执行被拒绝或超过最大尝试次数后的处理策略。
 */
public enum MessageIdempotencyFailurePolicy {
    /** 保守策略：不确认 Broker，让消息后续继续重投。 */
    RETRY,

    /** 标记 DISCARDED，并确认 Broker。适合非核心或明确可丢弃消息。 */
    DISCARD,

    /** 进入死信；v13 只保留配置语义。 */
    DEAD_LETTER
}
