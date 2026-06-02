package com.xjtu.iron.cache.config;

import com.xjtu.iron.cache.api.model.CacheSpec;

/**
 * 缓存策略变更监听器。
 *
 * <p>二期接入配置中心后，当某个 cacheName 的 TTL、开关、空值策略发生变化时，
 * 可以通过该监听器通知本地组件刷新策略。</p>
 */
public interface CacheSpecChangeListener {

    /** 处理指定 cacheName 的策略变更事件。 */
    void onChange(String cacheName, CacheSpec newSpec);
}
