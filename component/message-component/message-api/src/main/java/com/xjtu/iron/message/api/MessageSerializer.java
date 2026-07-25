package com.xjtu.iron.message.api;

/**
 * 定义业务对象与字节数组之间的序列化契约。
 */
public interface MessageSerializer {

    /**
     * 将业务对象序列化为消息字节。
     *
     * @param payload 业务对象
     * @return 消息字节
     */
    byte[] serialize(Object payload);

    /**
     * 将消息字节反序列化为指定业务类型。
     *
     * @param payload 消息字节
     * @param targetType 目标业务类型
     * @param <T> 目标类型
     * @return 反序列化后的业务对象
     */
    <T> T deserialize(byte[] payload, Class<T> targetType);
}
