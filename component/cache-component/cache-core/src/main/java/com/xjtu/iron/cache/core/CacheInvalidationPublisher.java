package com.xjtu.iron.cache.core;

import com.xjtu.iron.cache.api.CacheKey;

/**
 * 缓存失效消息发布器。
 *
 * <p>当某个实例执行 evict 后，可以通过这个接口发布失效消息，
 * 让其他实例删除自己的本地缓存。</p>
 *
 * <p>一期没有发布能力，使用 NoopCacheInvalidationPublisher。</p>
 * <p>二期 Redis Pub/Sub 会提供 Redis 实现。</p>
 */
public interface CacheInvalidationPublisher {

    /**
     * 发布缓存失效消息。
     *
     * @param key 需要失效的缓存 key
     */
    void publish(CacheKey key);
}