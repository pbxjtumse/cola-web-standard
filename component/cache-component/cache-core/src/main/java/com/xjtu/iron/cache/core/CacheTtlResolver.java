package com.xjtu.iron.cache.core;

import com.xjtu.iron.cache.api.key.CacheKey;
import com.xjtu.iron.cache.api.model.CacheSpec;

import java.time.Duration;

/**
 * 缓存 TTL 解析器。
 *
 * <p>TTL 不应该由 Provider 自己计算。</p>
 *
 * <p>原因：</p>
 *
 * <pre>
 * 1. TTL 是缓存策略的一部分，属于 core 层编排逻辑；
 * 2. Caffeine 和 Redis 应该使用同一份 TTL 结果；
 * 3. TTL 后续可能根据 key、cacheName、热点状态、配置中心动态变化；
 * 4. Provider 只负责存取，不负责策略决策。
 * </pre>
 */
public interface CacheTtlResolver {

    /**
     * 解析正常值缓存 TTL。
     *
     * @param key  缓存 key，后续可根据 key 做热点 TTL、特殊 TTL
     * @param spec 缓存策略
     * @return 最终 TTL
     */
    Duration resolveNormalTtl(CacheKey key, CacheSpec spec);

    /**
     * 解析空值缓存 TTL。
     *
     * @param key  缓存 key
     * @param spec 缓存策略
     * @return 空值 TTL
     */
    Duration resolveNullValueTtl(CacheKey key, CacheSpec spec);
}