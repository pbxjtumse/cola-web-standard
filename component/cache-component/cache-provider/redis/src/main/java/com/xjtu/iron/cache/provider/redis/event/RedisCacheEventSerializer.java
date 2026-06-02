package com.xjtu.iron.cache.provider.redis.event;

import com.xjtu.iron.cache.core.event.CacheEvent;

/**
 * Redis 缓存事件序列化器。
 *
 * <p>用于 Redis Pub/Sub 发布和接收 CacheEvent。</p>
 */
public interface RedisCacheEventSerializer {

    String serialize(CacheEvent event);

    CacheEvent deserialize(String text);
}