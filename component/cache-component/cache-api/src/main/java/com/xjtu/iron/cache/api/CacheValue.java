package com.xjtu.iron.cache.api;

import com.xjtu.iron.cache.api.enums.CacheLevel;
import lombok.Getter;

/**
 * Provider 层返回的缓存值。
 *  不能简单使用 Optional 表示缓存结果，因为缓存有三种状态
 * 1. 命中正常值
 * 2. 命中空值占位
 * 3. 未命中
 *
 * <p>Optional 只能表达有值和无值，无法区分“未命中”和“命中空值”。</p>
 */
@Getter
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

}