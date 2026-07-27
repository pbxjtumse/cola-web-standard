package com.xjtu.iron.message.spi;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Provider 交给 core 的原始入站消息。
 *
 * <p>{@code providerMessageId}：Provider 原生消息 ID</p>
 * <p>{@code messageKey}：Provider 返回的消息键</p>
 * <p>{@code headers}：完整线级消息头</p>
 * <p>{@code body}：原始消息体</p>
 * <p>{@code receivedAt}：Provider 收到消息的时间</p>
 * <p>{@code providerMetadata}：原生诊断元数据</p>
 */
public final class ProviderInboundMessage {
    /** Provider 原生消息 ID。 */
    private final String providerMessageId;

    /** Provider 返回的消息键。 */
    private final String messageKey;

    /** 完整线级消息头。 */
    private final Map<String, String> headers;

    /** 原始消息体。 */
    private final byte[] body;

    /** Provider 收到消息的时间。 */
    private final Instant receivedAt;

    /** 原生诊断元数据。 */
    private final Map<String, String> providerMetadata;


    /** 校验并防御性复制。 */
    public ProviderInboundMessage(
        String providerMessageId,
        String messageKey,
        Map<String, String> headers,
        byte[] body,
        Instant receivedAt,
        Map<String, String> providerMetadata) {
        messageKey = normalize(messageKey);
        headers = headers == null || headers.isEmpty()
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(headers));
        body = Objects.requireNonNull(body, "body must not be null").clone();
        receivedAt = Objects.requireNonNull(receivedAt, "receivedAt must not be null");
        providerMetadata = providerMetadata == null || providerMetadata.isEmpty()
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(providerMetadata));
    
        // 保存完成校验和标准化后的 providerMessageId。
        this.providerMessageId = providerMessageId;
        // 保存完成校验和标准化后的 messageKey。
        this.messageKey = messageKey;
        // 保存完成校验和标准化后的 headers。
        this.headers = headers;
        // 保存完成校验和标准化后的 body。
        this.body = body;
        // 保存完成校验和标准化后的 receivedAt。
        this.receivedAt = receivedAt;
        // 保存完成校验和标准化后的 providerMetadata。
        this.providerMetadata = providerMetadata;
    }

    public byte[] body() {
        return body.clone();
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
    /**
     * 返回Provider 原生消息 ID。
     *
     * @return Provider 原生消息 ID
     */
    public String providerMessageId() {
        // 返回不可变字段。
        return providerMessageId;
    }

    /**
     * 返回Provider 返回的消息键。
     *
     * @return Provider 返回的消息键
     */
    public String messageKey() {
        // 返回不可变字段。
        return messageKey;
    }

    /**
     * 返回完整线级消息头。
     *
     * @return 完整线级消息头
     */
    public Map<String, String> headers() {
        // 返回不可变字段。
        return headers;
    }

    /**
     * 返回Provider 收到消息的时间。
     *
     * @return Provider 收到消息的时间
     */
    public Instant receivedAt() {
        // 返回不可变字段。
        return receivedAt;
    }

    /**
     * 返回原生诊断元数据。
     *
     * @return 原生诊断元数据
     */
    public Map<String, String> providerMetadata() {
        // 返回不可变字段。
        return providerMetadata;
    }

    /**
     * 按全部字段比较两个值对象。
     *
     * @param object 待比较对象
     * @return 字段值全部一致时返回 true
     */
    @Override
    public boolean equals(Object object) {
        // 同一对象直接相等。
        if (this == object) {
            return true;
        }
        // 类型不同或对象为空时不相等。
        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        // 转换为当前类型后逐字段比较。
        ProviderInboundMessage other = (ProviderInboundMessage) object;
        return Objects.equals(providerMessageId, other.providerMessageId)
                && Objects.equals(messageKey, other.messageKey)
                && Objects.equals(headers, other.headers)
                && Objects.equals(body, other.body)
                && Objects.equals(receivedAt, other.receivedAt)
                && Objects.equals(providerMetadata, other.providerMetadata);
    }

    /**
     * 根据全部字段计算哈希值。
     *
     * @return 哈希值
     */
    @Override
    public int hashCode() {
        // 使用与 equals 相同的字段计算哈希值。
        return Objects.hash(providerMessageId, messageKey, headers, body, receivedAt, providerMetadata);
    }

    /**
     * 返回便于诊断的字段摘要。
     *
     * @return 字符串摘要
     */
    @Override
    public String toString() {
        // 拼接全部字段，保持值对象可诊断。
        return "ProviderInboundMessage{" +
                "providerMessageId=" + providerMessageId +
                ", messageKey=" + messageKey +
                ", headers=" + headers +
                ", body=" + body +
                ", receivedAt=" + receivedAt +
                ", providerMetadata=" + providerMetadata +
                '}';
    }

}
