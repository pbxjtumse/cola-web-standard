package com.xjtu.iron.cache.provider.redis.event;


import com.xjtu.iron.cache.core.event.CacheEvent;
import com.xjtu.iron.cache.core.event.CacheEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 基于 Redis Pub/Sub 的缓存事件发布器。
 *
 * <p>二期第一版用于广播本地缓存失效事件。</p>
 */
public class RedisCacheEventPublisher implements CacheEventPublisher {

    private final StringRedisTemplate stringRedisTemplate;

    private final RedisCacheEventSerializer serializer;

    private final String channel;

    public RedisCacheEventPublisher(
            StringRedisTemplate stringRedisTemplate,
            RedisCacheEventSerializer serializer,
            String channel
    ) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.serializer = serializer;
        this.channel = channel;
    }

    @Override
    public void publish(CacheEvent event) {
        String message = serializer.serialize(event);
        stringRedisTemplate.convertAndSend(channel, message);
    }
}
