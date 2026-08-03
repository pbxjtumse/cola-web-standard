package com.xjtu.iron.foundation.core.text;

/**
 * 提供基于 Unicode 码点的文本长度计算。
 *
 * <p>{@link String#length()} 统计 UTF-16 代码单元，可能把一个 Emoji 计算为两个字符；
 * 本类用于需要面向用户可见字符进行限制的技术场景。</p>
 */
public final class TextLength {

    private TextLength() {
    }

    /**
     * 计算 Unicode 码点数量。
     */
    public static int codePointLength(String value) {
        return value == null ? 0 : value.codePointCount(0, value.length());
    }

    /**
     * 判断文本码点数量是否未超过限制。
     */
    public static boolean isWithin(String value, int maxCodePoints) {
        if (maxCodePoints < 0) {
            throw new IllegalArgumentException("maxCodePoints must not be negative");
        }
        return codePointLength(value) <= maxCodePoints;
    }
}
