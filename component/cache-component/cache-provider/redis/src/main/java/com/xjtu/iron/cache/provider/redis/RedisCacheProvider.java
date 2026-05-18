package com.xjtu.iron.cache.provider.redis;


import com.xjtu.iron.cache.api.CacheKey;
import com.xjtu.iron.cache.api.CacheSpec;
import com.xjtu.iron.cache.api.CacheValue;
import com.xjtu.iron.cache.api.enums.CacheLevel;
import com.xjtu.iron.cache.core.CacheProvider;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;

public class RedisCacheProvider implements CacheProvider {

    private static final byte[] NULL_VALUE_BYTES =
            "__XJTU_IRON_CACHE_NULL__".getBytes(StandardCharsets.UTF_8);

    private final RedisBinaryClient redisClient;
    private final RedisCacheSerializer serializer;
    private final String keyPrefix;

    public RedisCacheProvider(
            RedisBinaryClient redisClient,
            RedisCacheSerializer serializer,
            String keyPrefix
    ) {
        this.redisClient = redisClient;
        this.serializer = serializer;
        this.keyPrefix = keyPrefix == null ? "" : keyPrefix;
    }

    @Override
    public String name() {
        return "redis";
    }

    @Override
    public <T> CacheValue<T> get(CacheKey key, Class<T> valueType, CacheSpec spec) {
        if (!spec.isEnableL2()) {
            return CacheValue.miss();
        }

        byte[] bytes = redisClient.get(toRedisKey(key));

        if (bytes == null) {
            return CacheValue.miss();
        }

        if (isNullValue(bytes)) {
            return CacheValue.nullValue(CacheLevel.L2);
        }

        T value = serializer.deserialize(bytes, valueType);

        return CacheValue.of(value, CacheLevel.L2);
    }

    @Override
    public void put(CacheKey key, Object value, Duration ttl, CacheSpec spec) {
        if (!spec.isEnableL2()) {
            return;
        }

        byte[] bytes = serializer.serialize(value);
        redisClient.set(toRedisKey(key), bytes, ttl);
    }

    @Override
    public void putNullValue(CacheKey key, Duration ttl, CacheSpec spec) {
        if (!spec.isEnableL2()) {
            return;
        }

        redisClient.set(toRedisKey(key), NULL_VALUE_BYTES, ttl);
    }

    @Override
    public void evict(CacheKey key) {
        redisClient.del(toRedisKey(key));
    }

    private String toRedisKey(CacheKey key) {
        return keyPrefix + key.fullKey();
    }

    private boolean isNullValue(byte[] bytes) {
        return Arrays.equals(NULL_VALUE_BYTES, bytes);
    }
}
