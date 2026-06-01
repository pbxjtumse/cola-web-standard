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
 * <p>作为 L1 缓存使用。</p>
 *
 * <p>适合场景：</p>
 *
 * <pre>
 * 字典
 * 配置
 * 规则
 * 热点读多写少数据
 * </pre>
 *
 * <p>不适合场景：</p>
 *
 * <pre>
 * 余额
 * 库存扣减
 * 支付状态强一致判断
 * 高频变化数据
 * </pre>
 *
 * <p>当前实现没有直接使用 Caffeine 的 expireAfterWrite，
 * 而是在 LocalCacheEntry 中保存 expireAtMillis。</p>
 *
 * <p>这样做是为了支持：</p>
 *
 * <pre>
 * 每个 key 独立 TTL
 * TTL 抖动
 * 空值 TTL
 * Redis 回填 L1 时使用不同 TTL
 * </pre>
 */
public class CaffeineCacheProvider implements CacheProvider {

    private final CaffeineCacheManager cacheManager;

    public CaffeineCacheProvider(CaffeineCacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Override
    public String name() {
        return "caffeine";
    }

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

    @Override
    public void put(CacheKey key, Object value, Duration ttl, CacheSpec spec) {
        if (!spec.isEnableL1()) {
            return;
        }

        Cache<String, LocalCacheEntry> cache = cacheManager.getCache(key.cacheName(), spec);
        long expireAtMillis = System.currentTimeMillis() + ttl.toMillis();

        cache.put(key.fullKey(), LocalCacheEntry.of(value, expireAtMillis));
    }

    @Override
    public void putNullValue(CacheKey key, Duration ttl, CacheSpec spec) {
        if (!spec.isEnableL1()) {
            return;
        }

        Cache<String, LocalCacheEntry> cache = cacheManager.getCache(key.cacheName(), spec);
        long expireAtMillis = System.currentTimeMillis() + ttl.toMillis();

        cache.put(key.fullKey(), LocalCacheEntry.nullValue(expireAtMillis));
    }

    @Override
    public void evict(CacheKey key) {
        Cache<String, LocalCacheEntry> cache =
                cacheManager.getCache(key.cacheName(), CacheSpec.defaults(key.cacheName()));

        cache.invalidate(key.fullKey());
    }
}
