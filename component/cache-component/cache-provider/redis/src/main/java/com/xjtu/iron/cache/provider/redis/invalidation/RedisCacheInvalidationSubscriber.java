package com.xjtu.iron.cache.provider.redis.invalidation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xjtu.iron.cache.api.CacheKey;
import com.xjtu.iron.cache.core.CacheInvalidationEvent;
import com.xjtu.iron.cache.core.LocalCacheInvalidator;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;

import java.nio.charset.StandardCharsets;

/**
 * Redis Pub/Sub 缓存失效事件订阅器。
 *
 * <p>这个类不命名为 MessageListener，是为了避免和后续 message-component 的概念混淆。</p>
 *
 * <p>它只是 Spring Data Redis 的 MessageListener 接口实现。</p>
 *
 * <p>职责：</p>
 *
 * <pre>
 * 1. 监听 Redis channel
 * 2. 解析 CacheInvalidationEvent
 * 3. 判断是否来自当前实例
 * 4. 如果不是当前实例发布的事件，则删除当前实例本地缓存
 * </pre>
 */
public class RedisCacheInvalidationSubscriber implements MessageListener {

    /**
     * JSON 反序列化器。
     */
    private final ObjectMapper objectMapper;

    /**
     * 本地缓存失效器。
     *
     * <p>通常是 CaffeineCacheProvider。</p>
     */
    private final LocalCacheInvalidator localCacheInvalidator;

    /**
     * 当前应用实例 ID。
     *
     * <p>用于跳过自己发布的失效事件。</p>
     */
    private final String instanceId;

    /**
     * 创建 Redis 缓存失效事件订阅器。
     *
     * @param objectMapper JSON 反序列化器
     * @param localCacheInvalidator 本地缓存失效器
     * @param instanceId 当前应用实例 ID
     */
    public RedisCacheInvalidationSubscriber(
            ObjectMapper objectMapper,
            LocalCacheInvalidator localCacheInvalidator,
            String instanceId
    ) {
        this.objectMapper = objectMapper;
        this.localCacheInvalidator = localCacheInvalidator;
        this.instanceId = instanceId;
    }

    /**
     * Redis Pub/Sub 消息回调方法。
     *
     * @param message Redis 消息
     * @param pattern 匹配模式，普通 channel 订阅时一般不用
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String payload = new String(message.getBody(), StandardCharsets.UTF_8);
            CacheInvalidationEvent event = objectMapper.readValue(payload, CacheInvalidationEvent.class);
            if (isSelfEvent(event)) {
                return;
            }
            CacheKey key = event.toCacheKey();
            localCacheInvalidator.invalidateLocal(key);

        } catch (Exception ignored) {
            /*
             * 订阅回调不能因为单条消息失败影响整个应用。
             *
             * 后续可以在这里接入：
             * 1. 日志
             * 2. metrics
             * 3. trace
             */
        }
    }

    /**
     * 判断事件是否由当前实例发布。
     *
     * @param event 缓存失效事件
     * @return true 表示当前实例发布的事件
     */
    private boolean isSelfEvent(CacheInvalidationEvent event) {
        return instanceId != null
                && instanceId.equals(event.getSourceInstanceId());
    }
}