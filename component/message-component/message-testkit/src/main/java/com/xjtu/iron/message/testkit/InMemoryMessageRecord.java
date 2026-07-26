package com.xjtu.iron.message.testkit;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 表示 InMemory Provider 已确认的一条线级消息记录。
 *
 * @param providerMessageId 内存 Provider 消息 ID
 * @param physicalDestination 物理目的地
 * @param messageId 组件消息 ID
 * @param key 消息键
 * @param headers 线级消息头
 * @param body 消息体
 * @param storedAt 保存时间
 */
public record InMemoryMessageRecord(
        String providerMessageId,
        String physicalDestination,
        String messageId,
        String key,
        Map<String, String> headers,
        byte[] body,
        Instant storedAt) {

    /**
     * 防御性复制可变字段。
     */
    public InMemoryMessageRecord {
        // 消息头复制为不可变 Map。
        headers = headers == null || headers.isEmpty()
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(headers));
        // 消息体复制为独立数组。
        body = body == null ? new byte[0] : body.clone();
    }

    /**
     * 返回消息体副本。
     */
    @Override
    public byte[] body() {
        // 防止测试代码修改历史记录。
        return body.clone();
    }
}
