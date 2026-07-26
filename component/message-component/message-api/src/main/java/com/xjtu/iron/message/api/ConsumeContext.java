package com.xjtu.iron.message.api;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 表示一次具体消费投递的运行时上下文。
 *
 * @param providerName 实际 Provider 名称
 * @param physicalDestination 实际物理目的地
 * @param consumerGroup 消费组或订阅名称
 * @param providerMessageId Provider 原生消息 ID
 * @param deliveryAttempt 当前投递次数；Provider 无法提供时通常为 1
 * @param receivedAt 组件收到消息的时间
 * @param metadata Provider 只读原生诊断元数据
 */
public record ConsumeContext(
        String providerName,
        String physicalDestination,
        String consumerGroup,
        String providerMessageId,
        int deliveryAttempt,
        Instant receivedAt,
        Map<String, String> metadata) {

    /**
     * 统一修正投递次数并复制元数据。
     */
    public ConsumeContext {
        // 对无法提供或非法的次数统一使用 1。
        deliveryAttempt = Math.max(1, deliveryAttempt);
        // 防止调用方修改 Provider 原始元数据。
        metadata = metadata == null || metadata.isEmpty()
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(metadata));
    }
}
