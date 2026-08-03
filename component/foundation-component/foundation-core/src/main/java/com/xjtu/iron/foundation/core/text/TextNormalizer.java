package com.xjtu.iron.foundation.core.text;

import org.apache.commons.lang3.StringUtils;

/**
 * 提供文本标准化能力。
 *
 * <p>标准化只处理技术层面的空白、换行和默认值，不改变业务文本本身的含义。</p>
 */
public final class TextNormalizer {

    private TextNormalizer() {
    }

    /**
     * 去除首尾空白，并将空字符串转换为 {@code null}。
     */
    public static String trimToNull(String value) {
        return StringUtils.trimToNull(value);
    }

    /**
     * 去除首尾空白，并将 {@code null} 转换为空字符串。
     */
    public static String trimToEmpty(String value) {
        return StringUtils.trimToEmpty(value);
    }

    /**
     * 去除首尾空白；处理结果为空时返回默认值。
     */
    public static String trimToDefault(String value, String defaultValue) {
        String normalized = trimToNull(value);
        return normalized == null ? defaultValue : normalized;
    }

    /**
     * 将 Windows 和旧 Mac 换行符统一转换为 Unix 换行符。
     */
    public static String normalizeLineEndings(String value) {
        if (value == null) {
            return null;
        }
        return value.replace("\r\n", "\n").replace('\r', '\n');
    }

    /**
     * 将连续空白字符压缩为一个普通空格。
     */
    public static String collapseWhitespace(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }
        return normalized.replaceAll("\\s+", " ");
    }
}
