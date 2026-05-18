package com.xjtu.iron.cache.core;

import com.xjtu.iron.cache.api.CacheKey;
import com.xjtu.iron.cache.api.CacheSpec;
import com.xjtu.iron.cache.api.CacheValue;

import java.time.Duration;

public interface CacheProvider {

    String name();

    <T> CacheValue<T> get(CacheKey key, Class<T> valueType, CacheSpec spec);

    void put(CacheKey key, Object value, Duration ttl, CacheSpec spec);

    void putNullValue(CacheKey key, Duration ttl, CacheSpec spec);

    void evict(CacheKey key);
}
