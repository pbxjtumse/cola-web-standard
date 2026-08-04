package com.xjtu.iron.message.demo.dto;

/**
 * Spring Boot Demo 的发送响应对象。
 */
public final class SendMessageResponse {

    private final String messageId;
    private final String providerMessageId;
    private final String topic;
    private final String status;

    public SendMessageResponse(
            String messageId,
            String providerMessageId,
            String topic,
            String status) {
        this.messageId = messageId;
        this.providerMessageId = providerMessageId;
        this.topic = topic;
        this.status = status;
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

    public String getStatus() {
        return status;
    }
}
