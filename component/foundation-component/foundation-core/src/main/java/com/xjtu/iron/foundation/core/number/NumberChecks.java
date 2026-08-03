package com.xjtu.iron.foundation.core.number;

import java.math.BigDecimal;

/**
 * 提供数字范围检查。
 */
public final class NumberChecks {

    private NumberChecks() {
    }

    public static boolean isPositive(long value) {
        return value > 0;
    }

    public static boolean isNonNegative(long value) {
        return value >= 0;
    }

    public static boolean isPositive(BigDecimal value) {
        return value != null && value.signum() > 0;
    }

    public static boolean isBetween(long value, long minInclusive, long maxInclusive) {
        if (minInclusive > maxInclusive) {
            throw new IllegalArgumentException("minInclusive must not exceed maxInclusive");
        }
        return value >= minInclusive && value <= maxInclusive;
    }
}
