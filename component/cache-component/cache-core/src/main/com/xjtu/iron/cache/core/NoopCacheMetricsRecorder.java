package com.xjtu.iron.cache.core;


import com.xjtu.iron.cache.api.CacheKey;
import com.xjtu.iron.cache.api.enums.CacheLevel;
import com.xjtu.iron.cache.api.enums.CacheOperation;

public class NoopCacheMetricsRecorder implements CacheMetricsRecorder {

    @Override
    public void recordHit(CacheKey key, CacheLevel level, long costMillis) {
    }

    @Override
    public void recordMiss(CacheKey key, long costMillis) {
    }

    @Override
    public void recordLoad(CacheKey key, long costMillis) {
    }

    @Override
    public void recordError(CacheKey key, CacheOperation operation, Throwable throwable) {
    }
}
