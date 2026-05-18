package com.xjtu.iron.cache.config;


import com.xjtu.iron.cache.api.CacheSpec;

public interface CacheSpecChangeListener {

    void onChange(String cacheName, CacheSpec newSpec);
}
