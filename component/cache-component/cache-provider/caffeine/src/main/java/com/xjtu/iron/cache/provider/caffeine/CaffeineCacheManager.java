package com.xjtu.iron.cache.provider.caffeine;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.xjtu.iron.cache.api.CacheSpec;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Caffeine Cache 管理器。
 *
 * <p>每个 cacheName 对应一个独立的 Caffeine Cache，避免不同业务缓存共享容量和统计。</p>
 */
public class CaffeineCacheManager {

    /** 默认最大本地缓存条目数。 */
    private final long defaultMaximumSize;

    /** cacheName 到 Caffeine Cache 的映射。 */
    private final Map<String, Cache<String, LocalCacheEntry>> caches = new ConcurrentHashMap<>();

    /** 创建管理器。 */
    public CaffeineCacheManager(long defaultMaximumSize) {
        this.defaultMaximumSize = defaultMaximumSize;
    }

    /** 获取指定 cacheName 的本地缓存，不存在时自动创建。 */
    public Cache<String, LocalCacheEntry> getCache(String cacheName, CacheSpec spec) {
        return caches.computeIfAbsent(cacheName, ignored -> Caffeine.newBuilder()
                .maximumSize(defaultMaximumSize)
                .recordStats()
                .build());
    }

    /** 清空指定 cacheName 的本地缓存。 */
    public void clear(String cacheName) {
        Cache<String, LocalCacheEntry> cache = caches.get(cacheName);
        if (cache != null) {
            cache.invalidateAll();
        }
    }
}
