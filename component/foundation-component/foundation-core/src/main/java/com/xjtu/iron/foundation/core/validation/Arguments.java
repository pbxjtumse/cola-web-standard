package com.xjtu.iron.foundation.core.validation;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/**
 * 方法入参校验工具。
 *
 * <p>该类面向组件内部防御式编程，不替代 Jakarta Validation，也不承载业务规则。</p>
 */
public final class Arguments {

    private Arguments() {
    }

    public static <T> T notNull(T value, String name) {
        return Objects.requireNonNull(value, name + " must not be null");
    }

    public static String notBlank(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }

    public static int positive(int value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }

    public static long positive(long value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }

    public static Duration positive(Duration value, String name) {
        notNull(value, name);
        if (value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }

    public static Duration notNegative(Duration value, String name) {
        notNull(value, name);
        if (value.isNegative()) {
            throw new IllegalArgumentException(name + " must not be negative");
        }
        return value;
    }

    public static <T extends Collection<?>> T notEmpty(T value, String name) {
        notNull(value, name);
        if (value.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be empty");
        }
        return value;
    }

    public static <T extends Map<?, ?>> T notEmpty(T value, String name) {
        notNull(value, name);
        if (value.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be empty");
        }
        return value;
    }
}
