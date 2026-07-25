package com.xjtu.iron.message.api.spi;

import com.xjtu.iron.message.api.MessageDestination;

import java.util.Map;

/**
 * 表示 Provider 交给 core 的原始入站消息。
 *
 * @param destination 逻辑消息目的地
 * @param nativeMessageId 中间件原生消息标识
 * @param key 消息键
 * @param headers 消息头
 * @param payload 原始消息字节
 * @param deliveryAttempt 当前投递次数
 */
public record ProviderInboundMessage(
        MessageDestination destination,
        String nativeMessageId,
        String key,
        Map<String, String> headers,
        byte[] payload,
        int deliveryAttempt) {

    /**
     * 对可变字段执行防御性复制和标准化。
     */
    public ProviderInboundMessage {
        // 消息头转换为不可变映射。
        headers = headers == null ? Map.of() : Map.copyOf(headers);
        // 消息体数组执行防御性复制。
        payload = payload == null ? null : payload.clone();
        // 投递次数至少为一次。
        deliveryAttempt = Math.max(1, deliveryAttempt);
    }

    /**
     * 返回消息体副本。
     *
     * @return 消息体副本
     */
    @Override
    public byte[] payload() {
        // 避免业务处理过程修改 Provider 持有的字节数组。
        return payload == null ? null : payload.clone();
    }
}
