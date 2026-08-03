package com.xjtu.iron.foundation.time;

import java.time.Duration;

/**
 * 提供持续时间边界运算。
 */
public final class DurationSupport {

    private DurationSupport() {
    }

    public static Duration min(Duration left, Duration right) {
        if (left == null || right == null) {
            throw new IllegalArgumentException("durations must not be null");
        }
        return left.compareTo(right) <= 0 ? left : right;
    }

    public static Duration max(Duration left, Duration right) {
        if (left == null || right == null) {
            throw new IllegalArgumentException("durations must not be null");
        }
        return left.compareTo(right) >= 0 ? left : right;
    }

    public static Duration clamp(Duration value, Duration min, Duration max) {
        if (value == null || min == null || max == null || min.compareTo(max) > 0) {
            throw new IllegalArgumentException("invalid duration range");
        }
        return max(min, min(value, max));
    }

    public static boolean isPositive(Duration value) {
        return value != null && !value.isZero() && !value.isNegative();
    }
}
