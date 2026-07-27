package com.xjtu.iron.message.testkit;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * InMemory Provider 已确认的一条线级消息记录。
 *
 * <p>{@code providerMessageId}：内存 Provider 消息 ID</p>
 * <p>{@code physicalDestination}：物理目的地</p>
 * <p>{@code messageId}：组件消息 ID</p>
 * <p>{@code messageKey}：业务实体键</p>
 * <p>{@code headers}：线级消息头</p>
 * <p>{@code body}：消息体</p>
 * <p>{@code storedAt}：保存时间</p>
 */
public final class InMemoryMessageRecord {
    /** 内存 Provider 消息 ID。 */
    private final String providerMessageId;

    /** 物理目的地。 */
    private final String physicalDestination;

    /** 组件消息 ID。 */
    private final String messageId;

    /** 业务实体键。 */
    private final String messageKey;

    /** 线级消息头。 */
    private final Map<String, String> headers;

    /** 消息体。 */
    private final byte[] body;

    /** 保存时间。 */
    private final Instant storedAt;


    public InMemoryMessageRecord(
        String providerMessageId,
        String physicalDestination,
        String messageId,
        String messageKey,
        Map<String, String> headers,
        byte[] body,
        Instant storedAt) {
        headers = headers == null || headers.isEmpty()
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(headers));
        body = body == null ? new byte[0] : body.clone();
    
        // 保存完成校验和标准化后的 providerMessageId。
        this.providerMessageId = providerMessageId;
        // 保存完成校验和标准化后的 physicalDestination。
        this.physicalDestination = physicalDestination;
        // 保存完成校验和标准化后的 messageId。
        this.messageId = messageId;
        // 保存完成校验和标准化后的 messageKey。
        this.messageKey = messageKey;
        // 保存完成校验和标准化后的 headers。
        this.headers = headers;
        // 保存完成校验和标准化后的 body。
        this.body = body;
        // 保存完成校验和标准化后的 storedAt。
        this.storedAt = storedAt;
    }

    public byte[] body() {
        return body.clone();
    }
    /**
     * 返回内存 Provider 消息 ID。
     *
     * @return 内存 Provider 消息 ID
     */
    public String providerMessageId() {
        // 返回不可变字段。
        return providerMessageId;
    }

    /**
     * 返回物理目的地。
     *
     * @return 物理目的地
     */
    public String physicalDestination() {
        // 返回不可变字段。
        return physicalDestination;
    }

    /**
     * 返回组件消息 ID。
     *
     * @return 组件消息 ID
     */
    public String messageId() {
        // 返回不可变字段。
        return messageId;
    }

    /**
     * 返回业务实体键。
     *
     * @return 业务实体键
     */
    public String messageKey() {
        // 返回不可变字段。
        return messageKey;
    }

    /**
     * 返回线级消息头。
     *
     * @return 线级消息头
     */
    public Map<String, String> headers() {
        // 返回不可变字段。
        return headers;
    }

    /**
     * 返回保存时间。
     *
     * @return 保存时间
     */
    public Instant storedAt() {
        // 返回不可变字段。
        return storedAt;
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
        InMemoryMessageRecord other = (InMemoryMessageRecord) object;
        return Objects.equals(providerMessageId, other.providerMessageId)
                && Objects.equals(physicalDestination, other.physicalDestination)
                && Objects.equals(messageId, other.messageId)
                && Objects.equals(messageKey, other.messageKey)
                && Objects.equals(headers, other.headers)
                && Objects.equals(body, other.body)
                && Objects.equals(storedAt, other.storedAt);
    }

    /**
     * 根据全部字段计算哈希值。
     *
     * @return 哈希值
     */
    @Override
    public int hashCode() {
        // 使用与 equals 相同的字段计算哈希值。
        return Objects.hash(providerMessageId, physicalDestination, messageId, messageKey, headers, body, storedAt);
    }

    /**
     * 返回便于诊断的字段摘要。
     *
     * @return 字符串摘要
     */
    @Override
    public String toString() {
        // 拼接全部字段，保持值对象可诊断。
        return "InMemoryMessageRecord{" +
                "providerMessageId=" + providerMessageId +
                ", physicalDestination=" + physicalDestination +
                ", messageId=" + messageId +
                ", messageKey=" + messageKey +
                ", headers=" + headers +
                ", body=" + body +
                ", storedAt=" + storedAt +
                '}';
    }

}
