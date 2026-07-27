package com.xjtu.iron.message.api;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 表示统一的消息发送结果。
 *
 * <p>{@code messageId}：组件消息 ID</p>
 * <p>{@code destination}：逻辑目的地</p>
 * <p>{@code providerName}：实际使用的 Provider</p>
 * <p>{@code physicalDestination}：实际物理目的地</p>
 * <p>{@code status}：发送状态</p>
 * <p>{@code stage}：结果产生阶段</p>
 * <p>{@code failureType}：标准失败类型</p>
 * <p>{@code providerMessageId}：Provider 或 Broker 返回的原生消息 ID</p>
 * <p>{@code description}：诊断描述</p>
 * <p>{@code startedAt}：发送开始时间</p>
 * <p>{@code completedAt}：发送完成时间</p>
 * <p>{@code metadata}：Provider 返回的只读诊断元数据</p>
 */
public final class SendResult {
    /** 组件消息 ID。 */
    private final String messageId;

    /** 逻辑目的地。 */
    private final MessageDestination destination;

    /** 实际使用的 Provider。 */
    private final String providerName;

    /** 实际物理目的地。 */
    private final String physicalDestination;

    /** 发送状态。 */
    private final SendStatus status;

    /** 结果产生阶段。 */
    private final SendStage stage;

    /** 标准失败类型。 */
    private final SendFailureType failureType;

    /** Provider 或 Broker 返回的原生消息 ID。 */
    private final String providerMessageId;

    /** 诊断描述。 */
    private final String description;

    /** 发送开始时间。 */
    private final Instant startedAt;

    /** 发送完成时间。 */
    private final Instant completedAt;

    /** Provider 返回的只读诊断元数据。 */
    private final Map<String, String> metadata;


    /**
     * 执行结果字段标准化和防御性复制。
     */
    public SendResult(
        String messageId,
        MessageDestination destination,
        String providerName,
        String physicalDestination,
        SendStatus status,
        SendStage stage,
        SendFailureType failureType,
        String providerMessageId,
        String description,
        Instant startedAt,
        Instant completedAt,
        Map<String, String> metadata) {
        // 没有元数据时统一使用空不可变 Map。
        metadata = metadata == null || metadata.isEmpty()
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(metadata));
    
        // 保存完成校验和标准化后的 messageId。
        this.messageId = messageId;
        // 保存完成校验和标准化后的 destination。
        this.destination = destination;
        // 保存完成校验和标准化后的 providerName。
        this.providerName = providerName;
        // 保存完成校验和标准化后的 physicalDestination。
        this.physicalDestination = physicalDestination;
        // 保存完成校验和标准化后的 status。
        this.status = status;
        // 保存完成校验和标准化后的 stage。
        this.stage = stage;
        // 保存完成校验和标准化后的 failureType。
        this.failureType = failureType;
        // 保存完成校验和标准化后的 providerMessageId。
        this.providerMessageId = providerMessageId;
        // 保存完成校验和标准化后的 description。
        this.description = description;
        // 保存完成校验和标准化后的 startedAt。
        this.startedAt = startedAt;
        // 保存完成校验和标准化后的 completedAt。
        this.completedAt = completedAt;
        // 保存完成校验和标准化后的 metadata。
        this.metadata = metadata;
    }

    /**
     * 判断消息是否已经获得明确成功确认。
     *
     * @return 已确认时返回 true
     */
    public boolean confirmed() {
        // 只有 CONFIRMED 才能被视为明确发送成功。
        return status == SendStatus.CONFIRMED;
    }

    /**
     * 判断结果是否不确定。
     *
     * @return 不确定时返回 true
     */
    public boolean unknown() {
        // UNKNOWN 不能等价为普通失败，也不能无条件重发。
        return status == SendStatus.UNKNOWN;
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
     * 返回逻辑目的地。
     *
     * @return 逻辑目的地
     */
    public MessageDestination destination() {
        // 返回不可变字段。
        return destination;
    }

    /**
     * 返回实际使用的 Provider。
     *
     * @return 实际使用的 Provider
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
     * 返回发送状态。
     *
     * @return 发送状态
     */
    public SendStatus status() {
        // 返回不可变字段。
        return status;
    }

    /**
     * 返回结果产生阶段。
     *
     * @return 结果产生阶段
     */
    public SendStage stage() {
        // 返回不可变字段。
        return stage;
    }

    /**
     * 返回标准失败类型。
     *
     * @return 标准失败类型
     */
    public SendFailureType failureType() {
        // 返回不可变字段。
        return failureType;
    }

    /**
     * 返回Provider 或 Broker 返回的原生消息 ID。
     *
     * @return Provider 或 Broker 返回的原生消息 ID
     */
    public String providerMessageId() {
        // 返回不可变字段。
        return providerMessageId;
    }

    /**
     * 返回诊断描述。
     *
     * @return 诊断描述
     */
    public String description() {
        // 返回不可变字段。
        return description;
    }

    /**
     * 返回发送开始时间。
     *
     * @return 发送开始时间
     */
    public Instant startedAt() {
        // 返回不可变字段。
        return startedAt;
    }

    /**
     * 返回发送完成时间。
     *
     * @return 发送完成时间
     */
    public Instant completedAt() {
        // 返回不可变字段。
        return completedAt;
    }

    /**
     * 返回Provider 返回的只读诊断元数据。
     *
     * @return Provider 返回的只读诊断元数据
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
        SendResult other = (SendResult) object;
        return Objects.equals(messageId, other.messageId)
                && Objects.equals(destination, other.destination)
                && Objects.equals(providerName, other.providerName)
                && Objects.equals(physicalDestination, other.physicalDestination)
                && Objects.equals(status, other.status)
                && Objects.equals(stage, other.stage)
                && Objects.equals(failureType, other.failureType)
                && Objects.equals(providerMessageId, other.providerMessageId)
                && Objects.equals(description, other.description)
                && Objects.equals(startedAt, other.startedAt)
                && Objects.equals(completedAt, other.completedAt)
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
        return Objects.hash(messageId, destination, providerName, physicalDestination, status, stage, failureType, providerMessageId, description, startedAt, completedAt, metadata);
    }

    /**
     * 返回便于诊断的字段摘要。
     *
     * @return 字符串摘要
     */
    @Override
    public String toString() {
        // 拼接全部字段，保持值对象可诊断。
        return "SendResult{" +
                "messageId=" + messageId +
                ", destination=" + destination +
                ", providerName=" + providerName +
                ", physicalDestination=" + physicalDestination +
                ", status=" + status +
                ", stage=" + stage +
                ", failureType=" + failureType +
                ", providerMessageId=" + providerMessageId +
                ", description=" + description +
                ", startedAt=" + startedAt +
                ", completedAt=" + completedAt +
                ", metadata=" + metadata +
                '}';
    }

}
