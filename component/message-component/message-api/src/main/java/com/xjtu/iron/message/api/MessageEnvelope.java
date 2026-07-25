package com.xjtu.iron.message.api;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 表示业务提交给消息组件的统一消息信封。
 *
 * @param messageId 消息唯一标识；允许业务不填写，由 core 自动生成
 * @param messageType 稳定的业务消息类型，例如 OrderPaid
 * @param payload 业务消息体
 * @param key 用于分区、顺序或业务定位的消息键
 * @param headers 业务扩展消息头
 * @param occurredAt 业务事件实际发生时间
 * @param <T> 业务消息体类型
 */
public record MessageEnvelope<T>(
        String messageId,
        String messageType,
        T payload,
        String key,
        Map<String, String> headers,
        Instant occurredAt) {

    /**
     * 执行防御性复制，保证消息信封创建后不会被外部修改。
     */
    public MessageEnvelope {
        // 空消息头被标准化为空只读映射。
        headers = headers == null ? Map.of() : Map.copyOf(headers);
    }

    /**
     * 创建仅包含消息类型和消息体的最小消息信封。
     *
     * @param messageType 业务消息类型
     * @param payload 业务消息体
     * @param <T> 消息体类型
     * @return 待 core 补全系统字段的消息信封
     */
    public static <T> MessageEnvelope<T> of(String messageType, T payload) {
        // messageId 和 occurredAt 统一交给 core 生成，避免各业务生成规则不一致。
        return new MessageEnvelope<>(null, messageType, payload, null, Map.of(), null);
    }

    /**
     * 返回带业务键的新消息信封。
     *
     * @param newKey 新业务键
     * @return 不可变的新消息信封
     */
    public MessageEnvelope<T> withKey(String newKey) {
        // record 不可变，因此通过新对象表达字段变化。
        return new MessageEnvelope<>(messageId, messageType, payload, newKey, headers, occurredAt);
    }

    /**
     * 返回合并指定业务消息头的新消息信封。
     *
     * @param additionalHeaders 需要增加或覆盖的业务消息头
     * @return 不可变的新消息信封
     */
    public MessageEnvelope<T> withHeaders(Map<String, String> additionalHeaders) {
        // 使用有序映射，便于测试和日志稳定输出。
        Map<String, String> mergedHeaders = new LinkedHashMap<>(headers);
        // 仅在调用方确实提供消息头时执行合并。
        if (additionalHeaders != null) {
            // 后加入的业务消息头覆盖同名旧值。
            mergedHeaders.putAll(additionalHeaders);
        }
        // 返回包含合并结果的新对象。
        return new MessageEnvelope<>(messageId, messageType, payload, key, mergedHeaders, occurredAt);
    }

    /**
     * 由 core 补齐系统字段并覆盖受保护的系统消息头。
     *
     * @param actualMessageId 最终消息唯一标识
     * @param actualOccurredAt 最终业务发生时间
     * @param systemHeaders core 生成的系统消息头
     * @return 已完成系统字段补齐的消息信封
     */
    public MessageEnvelope<T> enrich(
            String actualMessageId,
            Instant actualOccurredAt,
            Map<String, String> systemHeaders) {
        // 先复制业务消息头，保留业务自定义字段。
        Map<String, String> mergedHeaders = new LinkedHashMap<>(headers);
        // 系统消息头后写入，防止业务伪造 messageId 等受保护字段。
        mergedHeaders.putAll(systemHeaders);
        // 返回完整、不可变的消息信封。
        return new MessageEnvelope<>(
                actualMessageId,
                messageType,
                payload,
                key,
                mergedHeaders,
                actualOccurredAt);
    }
}
