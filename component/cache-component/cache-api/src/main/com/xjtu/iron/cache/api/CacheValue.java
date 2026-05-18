package com.xjtu.iron.cache.api;

import com.xjtu.iron.cache.api.enums.CacheLevel;

public class CacheValue<T> {

    private final boolean present;
    private final boolean nullValue;
    private final T value;
    private final CacheLevel level;

    private CacheValue(boolean present, boolean nullValue, T value, CacheLevel level) {
        this.present = present;
        this.nullValue = nullValue;
        this.value = value;
        this.level = level;
    }

    public static <T> CacheValue<T> miss() {
        return new CacheValue<>(false, false, null, CacheLevel.NONE);
    }

    public static <T> CacheValue<T> of(T value, CacheLevel level) {
        return new CacheValue<>(true, false, value, level);
    }

    public static <T> CacheValue<T> nullValue(CacheLevel level) {
        return new CacheValue<>(true, true, null, level);
    }

    public boolean isPresent() {
        return present;
    }

    public boolean isNullValue() {
        return nullValue;
    }

    public T getValue() {
        return value;
    }

    public CacheLevel getLevel() {
        return level;
    }
}