package com.xjtu.iron.foundation.core.number;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * 表示百分比值，例如 {@code 12.5%}。
 */
public final class Percentage implements Comparable<Percentage> {

    /** 百分比与比例转换使用的常量一百。 */
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    /** 以百分数形式保存的值，例如 12.5 表示 12.5%。 */
    private final BigDecimal value;

    private Percentage(BigDecimal value) {
        this.value = value.stripTrailingZeros();
    }

    /**
     * 根据百分数创建对象，例如传入 12.5 表示 12.5%。
     */
    public static Percentage of(BigDecimal value) {
        Objects.requireNonNull(value, "value must not be null");
        return new Percentage(value);
    }

    /**
     * 根据比例创建对象，例如传入 0.125 表示 12.5%。
     */
    public static Percentage fromRatio(BigDecimal ratio) {
        Objects.requireNonNull(ratio, "ratio must not be null");
        return new Percentage(ratio.multiply(HUNDRED));
    }

    public BigDecimal asPercent() {
        return value;
    }

    public BigDecimal asRatio(int scale, RoundingMode roundingMode) {
        return value.divide(HUNDRED, scale, roundingMode);
    }

    @Override
    public int compareTo(Percentage other) {
        return value.compareTo(other.value);
    }

    @Override
    public boolean equals(Object object) {
        return object instanceof Percentage other && value.compareTo(other.value) == 0;
    }

    @Override
    public int hashCode() {
        return value.stripTrailingZeros().hashCode();
    }

    @Override
    public String toString() {
        return value.toPlainString() + "%";
    }
}
