package com.xjtu.iron.cache.core;

import com.xjtu.iron.cache.api.CacheKey;
import com.xjtu.iron.cache.api.CacheSpec;

public interface CacheSpecResolver {

    CacheSpec resolve(CacheKey key);

    CacheSpec resolve(String cacheName);
}