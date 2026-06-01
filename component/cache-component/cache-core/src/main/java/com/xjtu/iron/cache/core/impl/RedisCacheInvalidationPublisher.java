package com.xjtu.iron.cache.core.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xjtu.iron.cache.api.CacheKey;
import com.xjtu.iron.cache.api.exception.CacheException;
import com.xjtu.iron.cache.core.CacheInvalidationEvent;
import com.xjtu.iron.cache.core.CacheInvalidationPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Redis Pub/Sub 缓存失效事件发布器。
 *
 * <p>它是 CacheInvalidationPublisher 的 Redis 实现。</p>
 *
 * <p>当业务主动调用 cacheClient.evict(key) 后，
 * CompositeCacheProvider 会调用本发布器，将失效事件发布到 Redis channel。</p>
 *
 * <p>注意：</p>
 *
 * <pre>
 * 这里只发布事件；
 * 不删除本地缓存；
 * 不删除 Redis key；
 * 删除动作由 CompositeCacheProvider 负责。
 * </pre>
 */
public class RedisCacheInvalidationPublisher implements CacheInvalidationPublisher {

    /**
     * 用于发布 Redis Pub/Sub 消息的 StringRedisTemplate。
     */
    private final StringRedisTemplate redisTemplate;

    /**
     * JSON 序列化器。
     *
     * <p>用于将 CacheInvalidationEvent 序列化为字符串。</p>
     */
    private final ObjectMapper objectMapper;

    /**
     * Redis Pub/Sub channel。
     */
    private final String channel;

    /**
     * 当前应用实例 ID。
     *
     * <p>用于让订阅端识别消息来源，避免重复处理自己发布的事件。</p>
     */
    private final String instanceId;

    /**
     * 创建 Redis 缓存失效事件发布器。
     *
     * @param redisTemplate Redis 字符串模板
     * @param objectMapper JSON 序列化器
     * @param channel Redis channel
     * @param instanceId 当前应用实例 ID
     */
    public RedisCacheInvalidationPublisher(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            String channel,
            String instanceId
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.channel = channel;
        this.instanceId = instanceId;
    }

    /**
     * 发布缓存失效事件。
     *
     * @param key 需要失效的缓存 key
     */
    @Override
    public void publish(CacheKey key) {
        try {
            CacheInvalidationEvent event = CacheInvalidationEvent.from(key, instanceId);
            String payload = objectMapper.writeValueAsString(event);

            redisTemplate.convertAndSend(channel, payload);

        } catch (Exception ex) {
            throw new CacheException("Publish cache invalidation event failed, key=" + key.fullKey(), ex);
        }
    }
}