package com.xjtu.iron.foundation.core.object;

import java.util.Objects;

/**
 * 提供对象引用组合检查。
 */
public final class ObjectChecks {

    private ObjectChecks() {
    }

    public static boolean allNonNull(Object... values) {
        if (values == null || values.length == 0) {
            return false;
        }
        for (Object value : values) {
            if (value == null) {
                return false;
            }
        }
        return true;
    }

    public static boolean anyNull(Object... values) {
        return !allNonNull(values);
    }

    public static <T> T requireNonNull(T value, String name) {
        return Objects.requireNonNull(value, name + " must not be null");
    }
}
