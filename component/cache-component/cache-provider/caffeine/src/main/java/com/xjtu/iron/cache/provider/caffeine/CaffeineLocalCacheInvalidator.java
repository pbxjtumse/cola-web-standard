package com.xjtu.iron.cache.provider.caffeine;


import com.xjtu.iron.cache.api.key.CacheKey;
import com.xjtu.iron.cache.core.invalidate.LocalCacheInvalidator;

/**
 * Caffeine 本地缓存失效器。
 *
 * <p>用于处理二期 Redis Pub/Sub 收到的本地缓存失效事件。</p>
 *
 * <p>它只删除当前实例的 Caffeine，不操作 Redis。</p>
 */
public class CaffeineLocalCacheInvalidator implements LocalCacheInvalidator {

    private final CaffeineLocalCacheManager cacheManager;

    public CaffeineLocalCacheInvalidator(CaffeineLocalCacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Override
    public void invalidateLocal(CacheKey key) {
        cacheManager.evict(key);
    }

    @Override
    public void invalidateLocalCacheName(String cacheName) {
        cacheManager.clear(cacheName);
    }
}
