package com.xjtu.iron.cache.core;

import com.xjtu.iron.cache.api.CacheKey;

/**
 * 本地缓存失效器。
 *
 * <p>用于删除当前 JVM 内的本地缓存。</p>
 *
 * <p>二期 Redis Pub/Sub 本地缓存失效通知会依赖这个接口。</p>
 *
 * <p>注意：</p>
 *
 * <pre>
 * LocalCacheInvalidator 只负责删除本地缓存，不负责删除 Redis。
 * </pre>
 */
public interface LocalCacheInvalidator {

    /**
     * 删除当前实例内的本地缓存。
     *
     * @param key 缓存 key
     */
    void invalidateLocal(CacheKey key);
}