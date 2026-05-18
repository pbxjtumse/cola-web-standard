package com.xjtu.iron.cache.integrations.observability;
import com.xjtu.iron.cache.api.CacheKey;
import com.xjtu.iron.cache.api.enums.CacheLevel;
import com.xjtu.iron.cache.api.enums.CacheOperation;
import com.xjtu.iron.cache.core.CacheMetricsRecorder;
import io.micrometer.core.instrument.MeterRegistry;

import java.util.concurrent.TimeUnit;

public class MicrometerCacheMetricsRecorder implements CacheMetricsRecorder {

    private final MeterRegistry meterRegistry;

    public MicrometerCacheMetricsRecorder(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void recordHit(CacheKey key, CacheLevel level, long costMillis) {
        meterRegistry.counter(
                "xjtu.iron.cache.hit",
                "cacheName", key.cacheName(),
                "level", level.name()
        ).increment();

        meterRegistry.timer(
                "xjtu.iron.cache.get.cost",
                "cacheName", key.cacheName(),
                "result", "hit",
                "level", level.name()
        ).record(costMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public void recordMiss(CacheKey key, long costMillis) {
        meterRegistry.counter(
                "xjtu.iron.cache.miss",
                "cacheName", key.cacheName()
        ).increment();

        meterRegistry.timer(
                "xjtu.iron.cache.get.cost",
                "cacheName", key.cacheName(),
                "result", "miss"
        ).record(costMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public void recordLoad(CacheKey key, long costMillis) {
        meterRegistry.counter(
                "xjtu.iron.cache.loader.count",
                "cacheName", key.cacheName()
        ).increment();

        meterRegistry.timer(
                "xjtu.iron.cache.loader.cost",
                "cacheName", key.cacheName()
        ).record(costMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public void recordError(CacheKey key, CacheOperation operation, Throwable throwable) {
        meterRegistry.counter(
                "xjtu.iron.cache.error",
                "cacheName", key.cacheName(),
                "operation", operation.name(),
                "exception", throwable.getClass().getSimpleName()
        ).increment();
    }
}
