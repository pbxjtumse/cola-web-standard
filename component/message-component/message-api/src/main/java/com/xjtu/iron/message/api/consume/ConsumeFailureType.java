package com.xjtu.iron.message.api.consume;

/**
 * 消费失败类型，用于日志、指标和后续死信治理。
 */
public enum ConsumeFailureType {
    NONE,
    DECODE_ERROR,
    CONSUMER_NOT_FOUND,
    HANDLER_ERROR,
    IDEMPOTENCY_CONFLICT,
    IDEMPOTENCY_STORAGE_ERROR,
    TRANSACTION_ERROR,
    ACK_ERROR,
    PROVIDER_ERROR,
    UNKNOWN_ERROR
}
