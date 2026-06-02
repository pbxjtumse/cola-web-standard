package com.xjtu.iron.cache.provider.caffeine;


import com.xjtu.iron.cache.api.key.CacheKey;
import com.xjtu.iron.cache.core.invalidate.LocalCacheInvalidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Caffeine 本地缓存失效器。
 *
 * <p>用于处理二期 Redis Pub/Sub 收到的本地缓存失效事件。</p>
 *
 * <p>它只删除当前实例的 Caffeine，不操作 Redis。</p>
 */
public class CaffeineLocalCacheInvalidator implements LocalCacheInvalidator {
    private static final Logger log = LoggerFactory.getLogger(CaffeineLocalCacheInvalidator.class);

    private final CaffeineLocalCacheManager cacheManager;

    public CaffeineLocalCacheInvalidator(CaffeineLocalCacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Override
    public void invalidateLocal(CacheKey key) {
        log.info("[LOCAL-CACHE-INVALIDATE] fullKey={}", key.fullKey());
        cacheManager.evict(key);
    }

    @Override
    public void invalidateLocalCacheName(String cacheName) {
        cacheManager.clear(cacheName);
    }
}
