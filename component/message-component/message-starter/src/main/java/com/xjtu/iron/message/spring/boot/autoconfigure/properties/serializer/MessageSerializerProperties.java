package com.xjtu.iron.message.spring.boot.autoconfigure.properties.serializer;

/**
 * 消息体序列化配置。
 *
 * <p>该配置只描述 message-core 选择哪一种 payload 序列化器，
 * 不负责消息线级协议中的系统消息头、上下文和目的地校验。</p>
 */
public class MessageSerializerProperties {

    /**
     * 序列化器类型。
     *
     * <p>当前默认使用 jackson。若业务需要统一复用 foundation-component，
     * 可以在业务应用中声明自己的 MessageSerializer Bean，Starter 会优先使用业务 Bean。</p>
     */
    private String type = "jackson";

    /**
     * 返回序列化器类型。
     *
     * @return 序列化器类型
     */
    public String getType() {
        return type;
    }

    /**
     * 设置序列化器类型。
     *
     * @param type 序列化器类型
     */
    public void setType(String type) {
        this.type = type;
    }
}
