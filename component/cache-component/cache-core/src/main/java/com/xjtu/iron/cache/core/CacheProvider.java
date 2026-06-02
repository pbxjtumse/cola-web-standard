package com.xjtu.iron.cache.core;

import com.xjtu.iron.cache.api.key.CacheKey;
import com.xjtu.iron.cache.api.model.CacheSpec;
import com.xjtu.iron.cache.api.model.CacheValue;

import java.time.Duration;

/**
 * 缓存 Provider SPI。
 *
 * <p>Provider 是 cache-core 面向具体缓存实现的扩展点。core 不直接依赖 Redis、Caffeine，
 * 而是通过 CacheProvider 统一操作不同缓存层。</p>
 */
public interface CacheProvider {

    /** 返回 Provider 名称，例如 caffeine、redis、composite。 */
    String name();

    /**
     * 读取缓存。
     *
     * @param key 缓存 key
     * @param valueType 目标值类型
     * @param spec 缓存策略
     * @return 缓存值，必须能表达命中、空值命中、未命中三种状态
     */
    <T> CacheValue<T> get(CacheKey key, Class<T> valueType, CacheSpec spec);

    /**
     * 写入正常值。
     *
     * @param key 缓存 key
     * @param value 要写入的正常值
     * @param ttl 已经由 core 计算好的 TTL，包含随机抖动结果
     * @param spec 缓存策略
     */
    void put(CacheKey key, Object value, Duration ttl, CacheSpec spec);

    /**
     * 写入空值占位。
     *
     * @param key 缓存 key
     * @param ttl 空值 TTL
     * @param spec 缓存策略
     */
    void putNullValue(CacheKey key, Duration ttl, CacheSpec spec);

    /** 删除缓存。 */
    void evict(CacheKey key);
}
