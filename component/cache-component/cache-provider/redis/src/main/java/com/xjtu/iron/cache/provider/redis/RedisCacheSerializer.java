package com.xjtu.iron.cache.provider.redis;


public interface RedisCacheSerializer {

    byte[] serialize(Object value);

    <T> T deserialize(byte[] bytes, Class<T> valueType);
}
