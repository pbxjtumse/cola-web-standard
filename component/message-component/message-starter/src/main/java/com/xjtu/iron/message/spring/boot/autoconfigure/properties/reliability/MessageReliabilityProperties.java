package com.xjtu.iron.message.spring.boot.autoconfigure.properties.reliability;

/**
 * 消息可靠性配置分组。
 *
 * <p>对应 {@code xjtu.iron.message.reliability.*}。当前只落地发送可靠性，
 * 消费可靠性后续由 consume 配置和 Provider ACK 映射共同承接。</p>
 */
public final class MessageReliabilityProperties {

    /** 发送可靠性配置。 */
    private MessageSendReliabilityProperties send = new MessageSendReliabilityProperties();

    public MessageSendReliabilityProperties getSend() {
        return send;
    }

    public void setSend(MessageSendReliabilityProperties send) {
        this.send = send == null ? new MessageSendReliabilityProperties() : send;
    }
}
