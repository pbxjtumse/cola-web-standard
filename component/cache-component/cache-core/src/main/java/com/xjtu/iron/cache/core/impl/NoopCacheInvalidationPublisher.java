package com.xjtu.iron.cache.core.impl;

import com.xjtu.iron.cache.api.CacheKey;
import com.xjtu.iron.cache.core.CacheInvalidationPublisher;

/**
 * 空实现缓存失效发布器。
 *
 * <p>当没有开启 Redis Pub/Sub 或消息组件时使用。</p>
 */
public class NoopCacheInvalidationPublisher implements CacheInvalidationPublisher {

    @Override
    public void publish(CacheKey key) {
        // no-op
    }
}