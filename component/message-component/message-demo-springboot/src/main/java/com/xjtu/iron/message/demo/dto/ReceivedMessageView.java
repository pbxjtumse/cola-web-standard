package com.xjtu.iron.message.demo.dto;

import java.time.Instant;
import java.util.Map;

/**
 * Spring Boot Demo 的已接收消息视图。
 */
public final class ReceivedMessageView {

    private final String messageId;
    private final String businessKey;
    private final String eventType;
    private final Object payload;
    private final Map<String, String> headers;
    private final Instant receivedAt;

    public ReceivedMessageView(
            String messageId,
            String businessKey,
            String eventType,
            Object payload,
            Map<String, String> headers,
            Instant receivedAt) {
        this.messageId = messageId;
        this.businessKey = businessKey;
        this.eventType = eventType;
        this.payload = payload;
        this.headers = headers;
        this.receivedAt = receivedAt;
    }

    public String getMessageId() {
        return messageId;
    }

    public String getBusinessKey() {
        return businessKey;
    }

    public String getEventType() {
        return eventType;
    }

    public Object getPayload() {
        return payload;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }
}
