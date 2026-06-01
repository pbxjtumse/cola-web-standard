package com.xjtu.iron.cache.provider.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.xjtu.iron.cache.api.exception.CacheSerializeException;

/**
 * 基于 Jackson 的 Redis 缓存序列化器。
 *
 * <p>一期优先使用 JSON，便于调试和排查。后续性能瓶颈明确后，可以扩展二进制序列化实现。</p>
 */
public class JacksonRedisCacheSerializer implements RedisCacheSerializer {

    /** Jackson 对象映射器。 */
    private final ObjectMapper objectMapper;

    /** 创建序列化器并注册 Java 8 时间类型模块。 */
    public JacksonRedisCacheSerializer() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /** 序列化对象。 */
    @Override
    public byte[] serialize(Object value) {
        try {
            return objectMapper.writeValueAsBytes(value);
        } catch (Exception ex) {
            throw new CacheSerializeException("Redis cache serialize failed", ex);
        }
    }

    /** 反序列化对象。 */
    @Override
    public <T> T deserialize(byte[] bytes, Class<T> valueType) {
        try {
            return objectMapper.readValue(bytes, valueType);
        } catch (Exception ex) {
            throw new CacheSerializeException("Redis cache deserialize failed, valueType=" + valueType.getName(), ex);
        }
    }
}
