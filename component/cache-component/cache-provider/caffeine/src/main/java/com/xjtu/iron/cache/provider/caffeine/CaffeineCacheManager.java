package com.xjtu.iron.cache.provider.caffeine;


import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.xjtu.iron.cache.api.CacheSpec;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CaffeineCacheManager {

    private final long defaultMaximumSize;

    private final Map<String, Cache<String, LocalCacheEntry>> caches = new ConcurrentHashMap<>();

    public CaffeineCacheManager(long defaultMaximumSize) {
        this.defaultMaximumSize = defaultMaximumSize;
    }

    public Cache<String, LocalCacheEntry> getCache(String cacheName, CacheSpec spec) {
        return caches.computeIfAbsent(cacheName, ignored -> Caffeine.newBuilder()
                .maximumSize(defaultMaximumSize)
                .recordStats()
                .build());
    }

    public void clear(String cacheName) {
        Cache<String, LocalCacheEntry> cache = caches.get(cacheName);

        if (cache != null) {
            cache.invalidateAll();
        }
    }
}
