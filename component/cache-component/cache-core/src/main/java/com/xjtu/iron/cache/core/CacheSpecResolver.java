package com.xjtu.iron.cache.core;

import com.xjtu.iron.cache.api.key.CacheKey;
import com.xjtu.iron.cache.api.model.CacheSpec;

/**
 * 缓存策略解析器。
 *
 * <p>一期默认从 Spring Boot 配置文件中读取策略；二期可以改为配置中心动态刷新。</p>
 */
public interface CacheSpecResolver {

    /** 根据 CacheKey 解析策略，通常内部使用 key.cacheName()。 */
    CacheSpec resolve(CacheKey key);

    /** 根据 cacheName 解析策略。 */
    CacheSpec resolve(String cacheName);
}
