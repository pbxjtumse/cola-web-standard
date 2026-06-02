package com.xjtu.iron.cache.provider.redis.event;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.xjtu.iron.cache.api.exception.CacheSerializeException;
import com.xjtu.iron.cache.core.event.CacheEvent;

/**
 * 基于 Jackson 的 Redis 缓存事件序列化器。
 */
public class JacksonRedisCacheEventSerializer implements RedisCacheEventSerializer {

    private final ObjectMapper objectMapper;

    public JacksonRedisCacheEventSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String serialize(CacheEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (Exception ex) {
            throw new CacheSerializeException("Serialize cache event failed", ex);
        }
    }

    @Override
    public CacheEvent deserialize(String text) {
        try {
            return objectMapper.readValue(text, CacheEvent.class);
        } catch (Exception ex) {
            throw new CacheSerializeException("Deserialize cache event failed", ex);
        }
    }
}
