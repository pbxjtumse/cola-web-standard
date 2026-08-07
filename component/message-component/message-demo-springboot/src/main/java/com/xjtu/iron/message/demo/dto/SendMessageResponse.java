package com.xjtu.iron.message.demo.dto;

import com.xjtu.iron.message.api.SendResult;

/**
 * Spring Boot Demo 的发送响应对象。
 */
public final class SendMessageResponse {

    /** 实际使用的 Provider。 */
    private final String providerName;

    /** 组件消息 ID。 */
    private final String messageId;

    /** Provider 原生消息 ID。 */
    private final String providerMessageId;

    /** 逻辑目的地。 */
    private final String topic;

    /** Provider 物理目的地。 */
    private final String physicalDestination;

    /** 发送状态。 */
    private final String status;

    /** 诊断描述。 */
    private final String description;

    /**
     * 兼容旧 Demo 的构造器。
     */
    public SendMessageResponse(
            String messageId,
            String providerMessageId,
            String topic,
            String status) {
        this(null, messageId, providerMessageId, topic, null, status, null);
    }

    public SendMessageResponse(
            String providerName,
            String messageId,
            String providerMessageId,
            String topic,
            String physicalDestination,
            String status,
            String description) {
        this.providerName = providerName;
        this.messageId = messageId;
        this.providerMessageId = providerMessageId;
        this.topic = topic;
        this.physicalDestination = physicalDestination;
        this.status = status;
        this.description = description;
    }

    /**
     * 从统一 SendResult 创建 Demo 响应。
     *
     * @param result 统一发送结果
     * @return Demo 响应
     */
    public static SendMessageResponse from(SendResult result) {
        return new SendMessageResponse(
                result.providerName(),
                result.messageId(),
                result.providerMessageId(),
                result.destination() == null ? null : result.destination().qualifiedName(),
                result.physicalDestination(),
                result.status() == null ? null : result.status().name(),
                result.description());
    }

    /**
     * 创建 Demo 层异常响应。
     */
    public static SendMessageResponse failed(
            String providerName,
            String topic,
            String description) {
        return new SendMessageResponse(
                providerName,
                null,
                null,
                topic,
                null,
                "FAILED",
                description);
    }

    public String getProviderName() {
        return providerName;
    }

    public String getMessageId() {
        return messageId;
    }

    public String getProviderMessageId() {
        return providerMessageId;
    }

    public String getTopic() {
        return topic;
    }

    public String getPhysicalDestination() {
        return physicalDestination;
    }

    public String getStatus() {
        return status;
    }

    public String getDescription() {
        return description;
    }
}
