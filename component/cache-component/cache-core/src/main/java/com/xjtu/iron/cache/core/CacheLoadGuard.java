package com.xjtu.iron.cache.core;

import java.util.concurrent.Callable;

/**
 * 缓存加载保护器。
 *
 * <p>用于在缓存 miss 后保护 loader，避免同一个 key 被大量并发请求同时加载源数据。</p>
 */
public interface CacheLoadGuard {

    /**
     * 在保护机制下执行 loader。
     *
     * @param key 加锁或保护所使用的资源 key
     * @param loader 实际要执行的数据加载逻辑
     */
    <T> T execute(String key, Callable<T> loader) throws Exception;
}
