package com.xjtu.iron.cache.core.event;


/**
 * 缓存事件发布器。
 *
 * <p>core 层只依赖这个抽象，不关心事件到底通过什么方式发布。</p>
 *
 * <p>二期第一版实现：</p>
 *
 * <pre>
 * RedisCacheEventPublisher
 * </pre>
 *
 * <p>默认实现：</p>
 *
 * <pre>
 * NoopCacheEventPublisher
 * </pre>
 */
public interface CacheEventPublisher {

    /**
     * 发布缓存事件。
     *
     * @param event 缓存事件
     */
    void publish(CacheEvent event);
}