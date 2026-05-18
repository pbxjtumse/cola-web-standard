package com.xjtu.iron.cache.provider.redis;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.xjtu.iron.cache.api.exception.CacheSerializeException;

public class JacksonRedisCacheSerializer implements RedisCacheSerializer {

    private final ObjectMapper objectMapper;

    public JacksonRedisCacheSerializer() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public byte[] serialize(Object value) {
        try {
            return objectMapper.writeValueAsBytes(value);
        } catch (Exception ex) {
            throw new CacheSerializeException("Redis cache serialize failed", ex);
        }
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> valueType) {
        try {
            return objectMapper.readValue(bytes, valueType);
        } catch (Exception ex) {
            throw new CacheSerializeException(
                    "Redis cache deserialize failed, valueType=" + valueType.getName(),
                    ex
            );
        }
    }
}
