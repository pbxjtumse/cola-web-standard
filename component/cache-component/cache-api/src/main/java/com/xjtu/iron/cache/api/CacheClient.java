package com.xjtu.iron.cache.api;

import java.time.Duration;
import java.util.Optional;

/**
 * 统一缓存访问入口。
 *
 * <p>这是业务代码唯一应该直接依赖的缓存门面。业务系统做普通业务缓存时，
 * 不建议直接注入 RedisTemplate、StringRedisTemplate、Caffeine Cache 或 RedissonClient。
 * 否则 key 规范、TTL、空值缓存、二级缓存回填、指标、降级等治理能力都会被绕开。</p>
 *
 * <p>一期默认实现是 {@code com.xjtu.iron.cache.core.impl.DefaultCacheClient}，
 * 默认底层链路是 L1 Caffeine + L2 Redis。</p>
 */
public interface CacheClient {

    /**
     * 按 cacheName 默认策略读取缓存。
     *
     * <p>如果缓存命中，直接返回缓存值；如果缓存未命中，则执行 loader 加载源数据，
     * 然后按照缓存策略回填 Redis 和 Caffeine。</p>
     *
     * @param key 缓存 key，包含 cacheName、namespace、bizKey、version
     * @param valueType 缓存值类型，用于 Redis 反序列化
     * @param loader 缓存未命中时的数据加载逻辑，通常是数据库查询、RPC 查询或外部接口查询
     * @return 缓存访问结果，包含 value、命中层级、命中状态、耗时等诊断信息
     */
    <T> CacheResult<T> get(CacheKey key, Class<T> valueType, CacheLoader<T> loader);

    /**
     * 按调用方显式传入的缓存策略读取缓存。
     *
     * <p>这个方法用于少数需要临时覆盖默认策略的场景。普通业务更推荐使用
     * {@link #get(CacheKey, Class, CacheLoader)}，让策略统一来自配置。</p>
     *
     * @param key 缓存 key
     * @param valueType 缓存值类型
     * @param spec 本次调用使用的缓存策略
     * @param loader 缓存未命中时的数据加载逻辑
     * @return 缓存访问结果
     */
    <T> CacheResult<T> get(CacheKey key, Class<T> valueType, CacheSpec spec, CacheLoader<T> loader);

    /**
     * 只读取缓存，不执行 loader。
     *
     * <p>适合只想判断缓存当前是否存在的场景。缓存不存在或命中空值占位时，返回 Optional.empty()。</p>
     *
     * @param key 缓存 key
     * @param valueType 缓存值类型
     * @return 当前缓存中的真实值；不会触发源数据加载
     */
    <T> Optional<T> getIfPresent(CacheKey key, Class<T> valueType);

    /**
     * 写入缓存，TTL 使用 cacheName 对应的默认策略。
     *
     * @param key 缓存 key
     * @param value 要写入的缓存值；如果为 null，会根据 nullPolicy 决定是否写空值占位
     */
    void put(CacheKey key, Object value);

    /**
     * 写入缓存，并由调用方指定 TTL。
     *
     * @param key 缓存 key
     * @param value 要写入的缓存值
     * @param ttl 本次写入使用的过期时间
     */
    void put(CacheKey key, Object value, Duration ttl);

    /**
     * 写入缓存，并由调用方指定完整缓存策略。
     *
     * @param key 缓存 key
     * @param value 要写入的缓存值
     * @param spec 本次写入使用的缓存策略
     */
    void put(CacheKey key, Object value, CacheSpec spec);

    /**
     * 删除缓存。
     *
     * <p>一期默认会同时删除 L1 Caffeine 和 L2 Redis。业务更新数据库成功后，
     * 通常应该调用 evict 删除缓存，而不是直接更新缓存。</p>
     *
     * @param key 要删除的缓存 key
     */
    void evict(CacheKey key);

    /**
     * 主动刷新缓存。
     *
     * <p>会执行 loader 获取最新数据，然后写入缓存。适合缓存预热、后台刷新、管理端手动刷新。</p>
     *
     * @param key 缓存 key
     * @param valueType 缓存值类型，当前一期主要用于语义表达
     * @param loader 最新数据加载逻辑
     */
    void refresh(CacheKey key, Class<?> valueType, CacheLoader<?> loader);
}
