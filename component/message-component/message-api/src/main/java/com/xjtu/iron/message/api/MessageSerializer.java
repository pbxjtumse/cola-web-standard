package com.xjtu.iron.message.api;

/**
 * 定义业务消息体与字节数组之间的序列化契约。
 */
public interface MessageSerializer {

    /**
     * 返回该序列化器生成的媒体类型。
     *
     * @return 媒体类型，例如 application/json
     */
    String contentType();

    /**
     * 将业务对象序列化为字节。
     *
     * @param payload 业务对象
     * @return 序列化字节
     */
    byte[] serialize(Object payload);

    /**
     * 将字节反序列化为指定业务类型。
     *
     * @param payload 序列化字节
     * @param targetType 目标类型
     * @param <T> 目标类型
     * @return 反序列化对象
     */
    <T> T deserialize(byte[] payload, Class<T> targetType);
}
