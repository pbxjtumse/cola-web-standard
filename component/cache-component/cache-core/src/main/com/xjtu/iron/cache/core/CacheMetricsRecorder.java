package com.xjtu.iron.cache.core;


import com.xjtu.iron.cache.api.CacheKey;
import com.xjtu.iron.cache.api.enums.CacheLevel;
import com.xjtu.iron.cache.api.enums.CacheOperation;

public interface CacheMetricsRecorder {

    void recordHit(CacheKey key, CacheLevel level, long costMillis);

    void recordMiss(CacheKey key, long costMillis);

    void recordLoad(CacheKey key, long costMillis);

    void recordError(CacheKey key, CacheOperation operation, Throwable throwable);
}
