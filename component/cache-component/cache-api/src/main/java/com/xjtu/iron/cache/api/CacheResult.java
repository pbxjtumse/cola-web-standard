package com.xjtu.iron.cache.api;

import com.xjtu.iron.cache.api.enums.CacheLevel;
import com.xjtu.iron.cache.api.enums.CacheResultStatus;

public class CacheResult<T> {

    private final T value;
    private final CacheResultStatus status;
    private final CacheLevel level;
    private final boolean hit;
    private final boolean nullValue;
    private final long costMillis;

    private CacheResult(
            T value,
            CacheResultStatus status,
            CacheLevel level,
            boolean hit,
            boolean nullValue,
            long costMillis
    ) {
        this.value = value;
        this.status = status;
        this.level = level;
        this.hit = hit;
        this.nullValue = nullValue;
        this.costMillis = costMillis;
    }

    public static <T> CacheResult<T> hit(CacheValue<T> cacheValue, long costMillis) {
        return new CacheResult<>(
                cacheValue.getValue(),
                CacheResultStatus.HIT,
                cacheValue.getLevel(),
                true,
                cacheValue.isNullValue(),
                costMillis
        );
    }

    public static <T> CacheResult<T> loaded(T value, long costMillis) {
        return new CacheResult<>(
                value,
                CacheResultStatus.MISS_LOADED,
                CacheLevel.SOURCE,
                false,
                value == null,
                costMillis
        );
    }

    public static <T> CacheResult<T> degraded(T value, long costMillis) {
        return new CacheResult<>(
                value,
                CacheResultStatus.DEGRADED,
                CacheLevel.NONE,
                false,
                value == null,
                costMillis
        );
    }

    public T getValue() {
        return value;
    }

    public CacheResultStatus getStatus() {
        return status;
    }

    public CacheLevel getLevel() {
        return level;
    }

    public boolean isHit() {
        return hit;
    }

    public boolean isNullValue() {
        return nullValue;
    }

    public long getCostMillis() {
        return costMillis;
    }
}
