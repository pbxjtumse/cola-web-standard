package com.xjtu.iron.cache.provider.caffeine;

import com.github.benmanes.caffeine.cache.Cache;
import com.xjtu.iron.cache.api.CacheKey;
import com.xjtu.iron.cache.api.CacheSpec;
import com.xjtu.iron.cache.api.CacheValue;
import com.xjtu.iron.cache.api.enums.CacheLevel;
import com.xjtu.iron.cache.core.CacheProvider;

import java.time.Duration;

/**
 * Caffeine 本地缓存 Provider。
 *
 * <p>作为 L1 缓存使用，适合字典、配置、规则、热点读多写少数据。</p>
 */
public class CaffeineCacheProvider implements CacheProvider {

    /** Caffeine Cache 管理器。 */
    private final CaffeineCacheManager cacheManager;

    /** 创建 Caffeine Provider。 */
    public CaffeineCacheProvider(CaffeineCacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    /** 返回 Provider 名称。 */
    @Override
    public String name() {
        return "caffeine";
    }

    /** 从 L1 本地缓存读取数据。 */
    @Override
    @SuppressWarnings("unchecked")
    public <T> CacheValue<T> get(CacheKey key, Class<T> valueType, CacheSpec spec) {
        if (!spec.isEnableL1()) {
            return CacheValue.miss();
        }

        Cache<String, LocalCacheEntry> cache = cacheManager.getCache(key.cacheName(), spec);
        LocalCacheEntry entry = cache.getIfPresent(key.fullKey());

        if (entry == null) {
            return CacheValue.miss();
        }

        if (entry.isExpired()) {
            cache.invalidate(key.fullKey());
            return CacheValue.miss();
        }

        if (entry.isNullValue()) {
            return CacheValue.nullValue(CacheLevel.L1);
        }

        return CacheValue.of((T) entry.getValue(), CacheLevel.L1);
    }

    /** 写入正常值到 L1。 */
    @Override
    public void put(CacheKey key, Object value, Duration ttl, CacheSpec spec) {
        if (!spec.isEnableL1()) {
            return;
        }
        Cache<String, LocalCacheEntry> cache = cacheManager.getCache(key.cacheName(), spec);
        long expireAtMillis = System.currentTimeMillis() + ttl.toMillis();
        cache.put(key.fullKey(), LocalCacheEntry.of(value, expireAtMillis));
    }

    /** 写入空值占位到 L1。 */
    @Override
    public void putNullValue(CacheKey key, Duration ttl, CacheSpec spec) {
        if (!spec.isEnableL1()) {
            return;
        }
        Cache<String, LocalCacheEntry> cache = cacheManager.getCache(key.cacheName(), spec);
        long expireAtMillis = System.currentTimeMillis() + ttl.toMillis();
        cache.put(key.fullKey(), LocalCacheEntry.nullValue(expireAtMillis));
    }

    /** 删除 L1 中的指定 key。 */
    @Override
    public void evict(CacheKey key) {
        Cache<String, LocalCacheEntry> cache = cacheManager.getCache(key.cacheName(), CacheSpec.defaults(key.cacheName()));
        cache.invalidate(key.fullKey());
    }
}
