package com.xjtu.iron.cache.api;

import java.time.Duration;
import java.util.Optional;

/**
 * 统一缓存访问入口。
 *
 * <p>业务系统不应该直接操作 RedisTemplate、Caffeine Cache 或 RedissonClient
 * 来完成普通业务缓存访问，而应该优先通过 CacheClient 访问缓存。</p>
 *
 * <p>CacheClient 负责屏蔽底层缓存实现，例如：
 * Caffeine 本地缓存、Redis 分布式缓存、多级缓存组合等。</p>
 *
 * <p>一期默认实现是 DefaultCacheClient，底层默认使用：
 * L1 Caffeine + L2 Redis。</p>
 */
public interface CacheClient {

    /**
     * 获取缓存。
     *
     * <p>如果缓存命中，直接返回缓存结果。</p>
     * <p>如果缓存未命中，则执行 loader 加载源数据，并根据缓存策略回填缓存。</p>
     *
     * @param key 缓存 key，包含 cacheName、namespace、bizKey、version
     * @param valueType 缓存值类型
     * @param loader 缓存未命中时的数据加载逻辑
     * @return 缓存访问结果，包含 value、命中层级、耗时等信息
     */
    <T> CacheResult<T> get(CacheKey key, Class<T> valueType, CacheLoader<T> loader);

    <T> CacheResult<T> get(CacheKey key, Class<T> valueType, CacheSpec spec, CacheLoader<T> loader);

    <T> Optional<T> getIfPresent(CacheKey key, Class<T> valueType);

    void put(CacheKey key, Object value);

    void put(CacheKey key, Object value, Duration ttl);

    void put(CacheKey key, Object value, CacheSpec spec);

    void evict(CacheKey key);

    void refresh(CacheKey key, Class<?> valueType, CacheLoader<?> loader);
}
