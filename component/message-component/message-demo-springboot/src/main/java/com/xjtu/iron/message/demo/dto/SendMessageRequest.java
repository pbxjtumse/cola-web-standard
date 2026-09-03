package com.xjtu.iron.message.demo.dto;

import java.util.Map;

/**
 * Spring Boot Demo 的发送请求对象。
 *
 * <p>这里使用普通 Java 类而不是 record，保持 message-component 当前代码风格一致。</p>
 */
public final class SendMessageRequest {

    /** 逻辑消息名称；Demo 中固定映射到 namespace=demo 下。 */
    private String topic;

    /** 可选 Provider 名称；为空时使用组件默认 Provider。 */
    private String provider;

    /** 业务实体键，对应 MessageEnvelope 的 messageKey。 */
    private String businessKey;

    /** 业务消息类型。 */
    private String eventType;

    /** 业务消息体。 */
    private Map<String, Object> payload;

    /** 用户扩展消息头。 */
    private Map<String, String> headers;

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getBusinessKey() {
        return businessKey;
    }

    public void setBusinessKey(String businessKey) {
        this.businessKey = businessKey;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }
}
