package com.xjtu.iron.cache.core.invalidate;


import com.xjtu.iron.cache.api.key.CacheKey;

/**
 * 本地缓存失效器。
 *
 * <p>它只负责删除当前 JVM 内的本地缓存。</p>
 *
 * <p>二期 Redis Pub/Sub 的核心逻辑是：</p>
 *
 * <pre>
 * 一个实例 evict 后发布事件；
 * 其他实例收到事件后，只删除自己的本地缓存；
 * 不再删除 Redis；
 * 不再继续发布事件。
 * </pre>
 */
public interface LocalCacheInvalidator {

    /**
     * 删除当前实例本地缓存中的单个 key。
     *
     * @param key 缓存 key
     */
    void invalidateLocal(CacheKey key);

    /**
     * 删除当前实例某个 cacheName 下的全部本地缓存。
     *
     * <p>二期第一版可以先不使用。</p>
     */
    void invalidateLocalCacheName(String cacheName);
}
