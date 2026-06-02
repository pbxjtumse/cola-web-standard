package com.xjtu.iron.cache.core.event;

/**
 * 空事件发布器。
 *
 * <p>当二期事件能力没有开启时，使用这个实现。</p>
 *
 * <p>它什么都不做，保证一期主链路不受影响。</p>
 */
public class NoopCacheEventPublisher implements CacheEventPublisher {

    @Override
    public void publish(CacheEvent event) {
        // no operation
    }
}