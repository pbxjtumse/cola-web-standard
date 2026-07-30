package com.xjtu.iron.message.api;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 表示一次具体消费投递的运行时上下文。
 *
 * <p>{@code providerName}：实际 Provider 名称</p>
 * <p>{@code physicalDestination}：实际物理目的地</p>
 * <p>{@code consumerGroup}：消费组或订阅名称</p>
 * <p>{@code providerMessageId}：Provider 原生消息 ID</p>
 * <p>{@code deliveryAttempt}：当前投递次数；Provider 无法提供时通常为 1</p>
 * <p>{@code receivedAt}：组件收到消息的时间</p>
 * <p>{@code metadata}：Provider 只读原生诊断元数据</p>
 */
public final class ConsumeContext {
    /** 实际 Provider 名称。 */
    private final String providerName;

    /** 实际物理目的地。 */
    private final String physicalDestination;

    /** 消费组或订阅名称。 */
    private final String consumerGroup;

    /** Provider 原生消息 ID。 */
    private final String providerMessageId;

    /** 组件收到消息的时间。 */
    private final Instant receivedAt;

    /** Provider 只读原生诊断元数据。 */
    private final Map<String, String> metadata;


    /**
     * 统一修正投递次数并复制元数据。
     */
    public ConsumeContext(
        String providerName,
        String physicalDestination,
        String consumerGroup,
        String providerMessageId,
        int deliveryAttempt,
        Instant receivedAt,
        Map<String, String> metadata) {
        // 对无法提供或非法的次数统一使用 1。
        deliveryAttempt = Math.max(1, deliveryAttempt);
        // 防止调用方修改 Provider 原始元数据。
        metadata = metadata == null || metadata.isEmpty()
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(metadata));
    
        // 保存完成校验和标准化后的 providerName。
        this.providerName = providerName;
        // 保存完成校验和标准化后的 physicalDestination。
        this.physicalDestination = physicalDestination;
        // 保存完成校验和标准化后的 consumerGroup。
        this.consumerGroup = consumerGroup;
        // 保存完成校验和标准化后的 providerMessageId。
        this.providerMessageId = providerMessageId;
        // 保存完成校验和标准化后的 receivedAt。
        this.receivedAt = receivedAt;
        // 保存完成校验和标准化后的 metadata。
        this.metadata = metadata;
    }
    /**
     * 返回实际 Provider 名称。
     *
     * @return 实际 Provider 名称
     */
    public String providerName() {
        // 返回不可变字段。
        return providerName;
    }

    /**
     * 返回实际物理目的地。
     *
     * @return 实际物理目的地
     */
    public String physicalDestination() {
        // 返回不可变字段。
        return physicalDestination;
    }

    /**
     * 返回消费组或订阅名称。
     *
     * @return 消费组或订阅名称
     */
    public String consumerGroup() {
        // 返回不可变字段。
        return consumerGroup;
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
     * 返回组件收到消息的时间。
     *
     * @return 组件收到消息的时间
     */
    public Instant receivedAt() {
        // 返回不可变字段。
        return receivedAt;
    }

    /**
     * 返回Provider 只读原生诊断元数据。
     *
     * @return Provider 只读原生诊断元数据
     */
    public Map<String, String> metadata() {
        // 返回不可变字段。
        return metadata;
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
        ConsumeContext other = (ConsumeContext) object;
        return Objects.equals(providerName, other.providerName)
                && Objects.equals(physicalDestination, other.physicalDestination)
                && Objects.equals(consumerGroup, other.consumerGroup)
                && Objects.equals(providerMessageId, other.providerMessageId)
                && Objects.equals(receivedAt, other.receivedAt)
                && Objects.equals(metadata, other.metadata);
    }

    /**
     * 根据全部字段计算哈希值。
     *
     * @return 哈希值
     */
    @Override
    public int hashCode() {
        // 使用与 equals 相同的字段计算哈希值。
        return Objects.hash(providerName, physicalDestination, consumerGroup, providerMessageId, receivedAt, metadata);
    }

    /**
     * 返回便于诊断的字段摘要。
     *
     * @return 字符串摘要
     */
    @Override
    public String toString() {
        // 拼接全部字段，保持值对象可诊断。
        return "ConsumeContext{" +
                "providerName=" + providerName +
                ", physicalDestination=" + physicalDestination +
                ", consumerGroup=" + consumerGroup +
                ", providerMessageId=" + providerMessageId +
                ", receivedAt=" + receivedAt +
                ", metadata=" + metadata +
                '}';
    }

}
