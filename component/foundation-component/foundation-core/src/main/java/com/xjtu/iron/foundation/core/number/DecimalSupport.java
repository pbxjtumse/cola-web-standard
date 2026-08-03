package com.xjtu.iron.foundation.core.number;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 提供统一精度和舍入规则的十进制运算。
 */
public final class DecimalSupport {

    private DecimalSupport() {
    }

    public static BigDecimal scale(BigDecimal value, int scale, RoundingMode roundingMode) {
        if (value == null) {
            return null;
        }
        if (scale < 0 || roundingMode == null) {
            throw new IllegalArgumentException("invalid scale or roundingMode");
        }
        return value.setScale(scale, roundingMode);
    }

    public static BigDecimal divide(BigDecimal dividend,
                                    BigDecimal divisor,
                                    int scale,
                                    RoundingMode roundingMode) {
        if (dividend == null || divisor == null) {
            throw new IllegalArgumentException("dividend and divisor must not be null");
        }
        if (divisor.signum() == 0) {
            throw new ArithmeticException("divisor must not be zero");
        }
        return dividend.divide(divisor, scale, roundingMode);
    }

    public static BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
