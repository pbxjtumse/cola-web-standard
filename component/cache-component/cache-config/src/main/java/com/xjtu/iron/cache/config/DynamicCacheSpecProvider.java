package com.xjtu.iron.cache.config;

import com.xjtu.iron.cache.api.CacheSpec;

import java.util.Map;

/**
 * 动态缓存策略提供者。
 *
 * <p>一期只预留接口；二期可以实现 Nacos、Apollo 或自研配置中心版本。</p>
 */
public interface DynamicCacheSpecProvider {

    /** 加载全部缓存策略。 */
    Map<String, CacheSpec> loadAll();

    /** 加载指定 cacheName 的缓存策略。 */
    CacheSpec load(String cacheName);

    /** 注册策略变更监听器。 */
    void addListener(CacheSpecChangeListener listener);
}
