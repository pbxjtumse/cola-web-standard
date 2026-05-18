package com.xjtu.iron.cache.config;


import com.xjtu.iron.cache.api.CacheKey;
import com.xjtu.iron.cache.api.CacheSpec;
import com.xjtu.iron.cache.core.CacheSpecResolver;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MapCacheSpecResolver implements CacheSpecResolver {

    private final Map<String, CacheSpec> specMap = new ConcurrentHashMap<>();

    public MapCacheSpecResolver() {
    }

    public MapCacheSpecResolver(Map<String, CacheSpec> specs) {
        if (specs != null) {
            this.specMap.putAll(specs);
        }
    }

    @Override
    public CacheSpec resolve(CacheKey key) {
        return resolve(key.cacheName());
    }

    @Override
    public CacheSpec resolve(String cacheName) {
        return specMap.getOrDefault(cacheName, CacheSpec.defaults(cacheName));
    }

    public void put(String cacheName, CacheSpec spec) {
        specMap.put(cacheName, spec);
    }

    public void remove(String cacheName) {
        specMap.remove(cacheName);
    }
}
