package com.xjtu.iron.cache.provider.caffeine;

import com.github.benmanes.caffeine.cache.Cache;
import com.xjtu.iron.cache.api.CacheKey;
import com.xjtu.iron.cache.api.CacheSpec;
import com.xjtu.iron.cache.api.CacheValue;
import com.xjtu.iron.cache.api.enums.CacheLevel;
import com.xjtu.iron.cache.core.CacheProvider;
import com.xjtu.iron.cache.core.LocalCacheInvalidator;

import java.time.Duration;

/**
 * Caffeine 本地缓存 Provider。
 *
 * <p>作为 L1 缓存使用，适合字典、配置、规则、热点读多写少数据。</p>
 * <p> 职责：</p>
 * <pre>
 * 1. 查询本地缓存
 * 2. 写入本地缓存
 * 3. 写入本地空值缓存
 * 4. 删除本地缓存
 * 5. 用于接收 Redis Pub/Sub 远程失效事件后 只删除当前实例的本地缓存
 * </pre>
 */
public class CaffeineCacheProvider implements CacheProvider, LocalCacheInvalidator {

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

    /**
     * 查询本地缓存。
     *
     * <p>如果当前 cacheName 没有开启 L1，则直接返回 miss。</p>
     *
     * <p>如果本地缓存存在但已经过期，则删除本地缓存并返回 miss。</p>
     *
     * @param key 缓存 key
     * @param valueType 值类型
     * @param spec 缓存策略
     * @return 缓存值
     */
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

    /**
     * 写入正常本地缓存。
     *
     * @param key 缓存 key
     * @param value 缓存值
     * @param ttl 过期时间
     * @param spec 缓存策略
     */
    @Override
    public void put(CacheKey key, Object value, Duration ttl, CacheSpec spec) {
        if (!spec.isEnableL1()) {
            return;
        }
        Cache<String, LocalCacheEntry> cache = cacheManager.getCache(key.cacheName(), spec);
        long expireAtMillis = System.currentTimeMillis() + ttl.toMillis();
        cache.put(key.fullKey(), LocalCacheEntry.of(value, expireAtMillis));
    }

    /**
     * 写入本地空值缓存。
     *
     * @param key 缓存 key
     * @param ttl 空值缓存过期时间
     * @param spec 缓存策略
     */
    @Override
    public void putNullValue(CacheKey key, Duration ttl, CacheSpec spec) {
        if (!spec.isEnableL1()) {
            return;
        }
        Cache<String, LocalCacheEntry> cache = cacheManager.getCache(key.cacheName(), spec);
        long expireAtMillis = System.currentTimeMillis() + ttl.toMillis();
        cache.put(key.fullKey(), LocalCacheEntry.nullValue(expireAtMillis));
    }

    /**
     * 删除本地缓存。
     *
     * <p>对于 Caffeine Provider 来说，evict 和 invalidateLocal 的实现可以一致。</p>
     *
     * @param key 缓存 key
     */
    @Override
    public void evict(CacheKey key) {
        invalidateLocal(key);
    }

    /**
     * 只删除当前 JVM 的本地缓存。
     *
     * <p>这个方法用于 Redis Pub/Sub 订阅端收到远程失效事件后的本地删除。</p>
     *
     * @param key 缓存 key
     */
    @Override
    public void invalidateLocal(CacheKey key) {
        Cache<String, LocalCacheEntry> cache = cacheManager.getCache(key.cacheName(), CacheSpec.defaults(key.cacheName()));
        cache.invalidate(key.fullKey());
    }
}
