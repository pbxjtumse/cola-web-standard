package com.xjtu.iron.foundation.core.text;

import java.util.Objects;

/**
 * 提供 Unicode 安全的文本截断能力。
 */
public final class TextTruncator {

    private TextTruncator() {
    }

    /**
     * 将文本截断到指定 Unicode 码点数量。
     */
    public static String truncate(String value, int maxCodePoints) {
        if (value == null) {
            return null;
        }
        if (maxCodePoints < 0) {
            throw new IllegalArgumentException("maxCodePoints must not be negative");
        }
        if (TextLength.codePointLength(value) <= maxCodePoints) {
            return value;
        }

        int endIndex = value.offsetByCodePoints(0, maxCodePoints);
        return value.substring(0, endIndex);
    }

    /**
     * 截断文本并附加后缀，最终长度不会超过限制。
     */
    public static String truncateWithSuffix(String value, int maxCodePoints, String suffix) {
        Objects.requireNonNull(suffix, "suffix must not be null");
        if (value == null) {
            return null;
        }
        if (TextLength.codePointLength(value) <= maxCodePoints) {
            return value;
        }

        int suffixLength = TextLength.codePointLength(suffix);
        if (suffixLength > maxCodePoints) {
            return truncate(suffix, maxCodePoints);
        }
        return truncate(value, maxCodePoints - suffixLength) + suffix;
    }
}
