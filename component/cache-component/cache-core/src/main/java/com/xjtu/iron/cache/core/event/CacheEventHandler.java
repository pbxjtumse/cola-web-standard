package com.xjtu.iron.cache.core.event;


/**
 * 缓存事件处理器。
 *
 * <p>Redis Pub/Sub 订阅到消息后，不应该直接把处理逻辑写在 Redis listener 里。</p>
 *
 * <p>Redis listener 只负责：</p>
 *
 * <pre>
 * 1. 接收消息；
 * 2. 反序列化成 CacheEvent；
 * 3. 交给 CacheEventHandler 处理。
 * </pre>
 */
public interface CacheEventHandler {

    /**
     * 处理缓存事件。
     *
     * @param event 缓存事件
     */
    void handle(CacheEvent event);
}
