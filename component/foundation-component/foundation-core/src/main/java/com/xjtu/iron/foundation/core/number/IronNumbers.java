package com.xjtu.iron.foundation.core.number;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

/**
 * 数字工具统一门面。
 */
public final class IronNumbers {

    private IronNumbers() {
    }

    public static int toIntExact(long value, String name) {
        if (value > Integer.MAX_VALUE || value < Integer.MIN_VALUE) {
            throw new IllegalArgumentException(name + " is out of int range: " + value);
        }
        return (int) value;
    }

    public static Optional<Integer> parseInt(String value) {
        try {
            return value == null || value.isBlank() ? Optional.empty() : Optional.of(Integer.parseInt(value.trim()));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    public static Optional<Long> parseLong(String value) {
        try {
            return value == null || value.isBlank() ? Optional.empty() : Optional.of(Long.parseLong(value.trim()));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    public static boolean equalsByCompare(BigDecimal left, BigDecimal right) {
        if (left == null || right == null) {
            return left == right;
        }
        return left.compareTo(right) == 0;
    }

    public static BigDecimal percentage(BigDecimal value, BigDecimal total, int scale) {
        if (value == null || total == null || BigDecimal.ZERO.compareTo(total) == 0) {
            return BigDecimal.ZERO;
        }
        return value.multiply(BigDecimal.valueOf(100)).divide(total, scale, RoundingMode.HALF_UP);
    }
}
