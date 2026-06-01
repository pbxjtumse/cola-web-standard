package com.xjtu.iron.cache.provider.redis;

/**
 * Redis 缓存序列化器。
 */
public interface RedisCacheSerializer {

    /** 将对象序列化为 byte[]。 */
    byte[] serialize(Object value);

    /** 将 byte[] 反序列化为指定类型对象。 */
    <T> T deserialize(byte[] bytes, Class<T> valueType);
}
