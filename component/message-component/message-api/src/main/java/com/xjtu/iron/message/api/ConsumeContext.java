package com.xjtu.iron.message.api;

import java.time.Instant;
import java.util.Map;

/**
 * 表示一次消息消费所需的非业务上下文。
 *
 * @param destination 逻辑消息目的地
 * @param providerName 实际接收消息的 Provider
 * @param nativeMessageId 中间件原生消息标识
 * @param deliveryAttempt 当前投递次数；无法获得时为 1
 * @param receivedAt 组件收到消息的时间
 * @param headers 完整消息头
 */
public record ConsumeContext(
        MessageDestination destination,
        String providerName,
        String nativeMessageId,
        int deliveryAttempt,
        Instant receivedAt,
        Map<String, String> headers) {

    /**
     * 对消息头执行防御性复制。
     */
    public ConsumeContext {
        // 避免 Provider 在业务处理期间修改上下文。
        headers = headers == null ? Map.of() : Map.copyOf(headers);
        // 投递次数至少按第一次投递处理。
        deliveryAttempt = Math.max(1, deliveryAttempt);
    }
}
