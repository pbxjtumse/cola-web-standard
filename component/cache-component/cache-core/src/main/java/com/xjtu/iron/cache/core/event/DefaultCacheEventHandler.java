package com.xjtu.iron.cache.core.event;


import com.xjtu.iron.cache.core.invalidate.LocalCacheInvalidator;

/**
 * 默认缓存事件处理器。
 *
 * <p>二期第一版只处理 EVICT_KEY。</p>
 *
 * <p>收到 EVICT_KEY 后，只删除当前实例的本地缓存。</p>
 */
public class DefaultCacheEventHandler implements CacheEventHandler {

    private final LocalCacheInvalidator localCacheInvalidator;

    private final String currentInstanceId;

    public DefaultCacheEventHandler(
            LocalCacheInvalidator localCacheInvalidator,
            String currentInstanceId
    ) {
        this.localCacheInvalidator = localCacheInvalidator;
        this.currentInstanceId = currentInstanceId;
    }

    @Override
    public void handle(CacheEvent event) {
        if (event == null || event.getEventType() == null) {
            return;
        }

        /*
         * 如果事件是当前实例自己发布的，可以忽略。
         *
         * 当前实例在 cacheClient.evict 时已经删除过本地缓存。
         */
        if (currentInstanceId != null && currentInstanceId.equals(event.getSourceInstanceId())) {
            return;
        }

        if (event.getEventType() == CacheEventType.EVICT_KEY) {
            localCacheInvalidator.invalidateLocal(event.toCacheKey());
        }

        if (event.getEventType() == CacheEventType.CLEAR_CACHE_NAME) {
            localCacheInvalidator.invalidateLocalCacheName(event.getCacheName());
        }
    }
}
