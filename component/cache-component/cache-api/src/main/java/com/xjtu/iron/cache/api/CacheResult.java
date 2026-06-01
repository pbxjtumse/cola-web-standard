package com.xjtu.iron.cache.api;

import com.xjtu.iron.cache.api.enums.CacheLevel;
import com.xjtu.iron.cache.api.enums.CacheResultStatus;
/**
 * CacheClient 对业务返回的缓存访问结果。
 *
 *  相比直接返回 value，CacheResult 能表达更多诊断信息：
 *
 * <pre>
 * 是否命中
 * 命中层级，L1 / L2 / SOURCE
 * 是否为空值缓存
 * 本次访问耗时
 * 访问状态
 * </pre>
 */
public class CacheResult<T> {

    /**
     * 缓存值
     */
    private final T value;

    /**
     * 访问状态
     */
    private final CacheResultStatus status;
    /**
     * 命中层级，L1 / L2 / SOURCE
     */
    private final CacheLevel level;
    /**
     * 是否命中
     */
    private final boolean hit;
    /**
     * 是否空值
     */
    private final boolean nullValue;
    /**
     * 超时时间？？？
     */
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
