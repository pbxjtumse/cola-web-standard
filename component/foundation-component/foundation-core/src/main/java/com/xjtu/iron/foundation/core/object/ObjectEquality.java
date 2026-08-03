package com.xjtu.iron.foundation.core.object;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Objects;

/**
 * 提供常见对象值比较能力。
 */
public final class ObjectEquality {

    private ObjectEquality() {
    }

    public static boolean equals(Object left, Object right) {
        return Objects.deepEquals(left, right);
    }

    /**
     * 忽略 BigDecimal 小数位精度进行数值比较。
     */
    public static boolean decimalEquals(BigDecimal left, BigDecimal right) {
        if (left == null || right == null) {
            return left == right;
        }
        return left.compareTo(right) == 0;
    }

    public static boolean byteArrayEquals(byte[] left, byte[] right) {
        return Arrays.equals(left, right);
    }
}
