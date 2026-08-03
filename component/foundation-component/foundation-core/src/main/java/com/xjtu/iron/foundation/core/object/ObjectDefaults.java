package com.xjtu.iron.foundation.core.object;

import java.util.function.Supplier;

/**
 * 提供延迟计算的对象默认值能力。
 */
public final class ObjectDefaults {

    private ObjectDefaults() {
    }

    public static <T> T defaultIfNull(T value, T defaultValue) {
        return value == null ? defaultValue : value;
    }

    public static <T> T defaultIfNull(T value, Supplier<? extends T> supplier) {
        if (value != null) {
            return value;
        }
        if (supplier == null) {
            throw new IllegalArgumentException("supplier must not be null");
        }
        return supplier.get();
    }
}
