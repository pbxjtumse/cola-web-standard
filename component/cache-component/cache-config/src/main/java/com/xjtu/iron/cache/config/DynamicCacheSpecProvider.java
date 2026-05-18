package com.xjtu.iron.cache.config;


import com.xjtu.iron.cache.api.CacheSpec;

import java.util.Map;

public interface DynamicCacheSpecProvider {

    Map<String, CacheSpec> loadAll();

    CacheSpec load(String cacheName);

    void addListener(CacheSpecChangeListener listener);
}
