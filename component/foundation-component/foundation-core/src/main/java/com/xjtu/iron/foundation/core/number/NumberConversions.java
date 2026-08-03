package com.xjtu.iron.foundation.core.number;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * 提供带溢出检查的数字转换。
 */
public final class NumberConversions {

    private NumberConversions() {
    }

    public static int toIntExact(long value) {
        return Math.toIntExact(value);
    }

    public static short toShortExact(int value) {
        if (value < Short.MIN_VALUE || value > Short.MAX_VALUE) {
            throw new ArithmeticException("short overflow: " + value);
        }
        return (short) value;
    }

    public static long toLongExact(BigInteger value) {
        if (value == null) {
            throw new IllegalArgumentException("value must not be null");
        }
        return value.longValueExact();
    }

    public static BigDecimal toBigDecimal(Number value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof BigInteger integer) {
            return new BigDecimal(integer);
        }
        return new BigDecimal(value.toString());
    }
}
