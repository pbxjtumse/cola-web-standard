package com.xjtu.iron.message.spi;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 表示 core 交给具体 Provider 的普通消息发送请求。
 *
 * @param destination 已解析物理目的地
 * @param messageId 组件消息 ID
 * @param key 消息键
 * @param headers 完整线级消息头，包含系统头和用户头
 * @param body 已序列化消息体
 */
public record ProviderSendRequest(
        ProviderDestination destination,
        String messageId,
        String key,
        Map<String, String> headers,
        byte[] body) {

    /**
     * 校验请求并复制可变数据。
     */
    public ProviderSendRequest {
        // 物理目的地不能为空。
        destination = Objects.requireNonNull(destination, "destination must not be null");
        // messageId 是 Provider 日志和幂等诊断的重要字段。
        if (messageId == null || messageId.isBlank()) {
            // core 必须在进入 SPI 前完成 ID 丰富。
            throw new IllegalArgumentException("messageId must not be blank");
        }
        // 复制消息头，避免 Provider 异步发送期间被修改。
        headers = headers == null || headers.isEmpty()
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(headers));
        // 消息体不能为空。
        body = Objects.requireNonNull(body, "body must not be null").clone();
    }

    /**
     * 返回消息体副本。
     *
     * @return 消息体副本
     */
    @Override
    public byte[] body() {
        // 防止调用方修改内部数组。
        return body.clone();
    }
}
