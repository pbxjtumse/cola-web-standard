package com.xjtu.iron.message.spi;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 表示 Provider 交给 core 的原始入站消息。
 *
 * @param providerMessageId Provider 原生消息 ID
 * @param key 消息键
 * @param headers 完整线级消息头
 * @param body 原始消息体
 * @param deliveryAttempt 当前投递次数
 * @param receivedAt Provider 收到消息的时间
 * @param metadata 原生诊断元数据
 */
public record ProviderInboundMessage(
        String providerMessageId,
        String key,
        Map<String, String> headers,
        byte[] body,
        int deliveryAttempt,
        Instant receivedAt,
        Map<String, String> metadata) {

    /**
     * 校验并复制入站消息。
     */
    public ProviderInboundMessage {
        // 原生消息 ID 在部分 Provider 中可能为空，因此不强制。
        // 复制消息头，避免底层客户端复用原始对象造成数据变化。
        headers = headers == null || headers.isEmpty()
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(headers));
        // 入站消息体必须存在。
        body = Objects.requireNonNull(body, "body must not be null").clone();
        // 无法获得准确次数时统一为 1。
        deliveryAttempt = Math.max(1, deliveryAttempt);
        // receivedAt 不能为空，Provider 应使用统一时钟或当前时间生成。
        receivedAt = Objects.requireNonNull(receivedAt, "receivedAt must not be null");
        // 元数据执行防御性复制。
        metadata = metadata == null || metadata.isEmpty()
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(metadata));
    }

    /**
     * 返回消息体副本。
     *
     * @return 消息体副本
     */
    @Override
    public byte[] body() {
        // 防止 core 或业务代码修改底层数组。
        return body.clone();
    }
}
