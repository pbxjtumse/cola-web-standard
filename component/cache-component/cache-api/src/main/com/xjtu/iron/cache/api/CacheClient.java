package com.xjtu.iron.cache.api;

import java.time.Duration;
import java.util.Optional;

public interface CacheClient {

    <T> CacheResult<T> get(CacheKey key, Class<T> valueType, CacheLoader<T> loader);

    <T> CacheResult<T> get(CacheKey key, Class<T> valueType, CacheSpec spec, CacheLoader<T> loader);

    <T> Optional<T> getIfPresent(CacheKey key, Class<T> valueType);

    void put(CacheKey key, Object value);

    void put(CacheKey key, Object value, Duration ttl);

    void put(CacheKey key, Object value, CacheSpec spec);

    void evict(CacheKey key);

    void refresh(CacheKey key, Class<?> valueType, CacheLoader<?> loader);
}
