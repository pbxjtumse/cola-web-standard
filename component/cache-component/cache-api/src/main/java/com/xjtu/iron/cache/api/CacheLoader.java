package com.xjtu.iron.cache.api;

/**
 * 缓存未命中时的数据加载函数。
 *
 * <p>CacheLoader 由业务方提供，缓存组件只负责在 miss 后调用它。
 * 它可以访问数据库、RPC、HTTP 接口、Repository 或其他权威数据源。</p>
 *
 * @param <T> 加载出的数据类型
 */
@FunctionalInterface
public interface CacheLoader<T> {

    /**
     * 加载源数据。
     *
     * @return 源数据；允许返回 null，是否缓存 null 由 CacheSpec.nullPolicy 决定
     * @throws Exception 加载失败时抛出，DefaultCacheClient 会统一包装为 CacheLoadException
     */
    T load() throws Exception;
}
