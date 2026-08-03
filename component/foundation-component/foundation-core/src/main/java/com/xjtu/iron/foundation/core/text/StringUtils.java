package com.xjtu.iron.foundation.core.text;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Foundation 字符串工具门面。
 *
 * <p>该类不是重新实现一套大型字符串工具库，而是对 JDK 与 Apache Commons Lang
 * 中高频能力做一层很薄的统一入口。所有方法均为无状态纯函数，适合被消息、重试、锁、缓存等
 * 技术组件直接调用。</p>
 *
 * <p>约定：这里不处理 SQL 转义、HTML 转义、JSON 转义、模板引擎等专项能力；那些能力应由
 * 更专业的组件或库处理。</p>
 */
public final class StringUtils {

    private static final Pattern LINE_ENDING_PATTERN = Pattern.compile("\\r\\n|\\r");
    private static final Pattern MULTI_WHITESPACE_PATTERN = Pattern.compile("\\s+");

    private StringUtils() {
    }

    /**
     * 判断字符串是否为 null、空串或全空白字符。
     *
     * @param value 待判断字符串
     * @return 为空白时返回 true
     */
    public static boolean isBlank(String value) {
        return org.apache.commons.lang3.StringUtils.isBlank(value);
    }

    /**
     * 判断字符串是否包含至少一个非空白字符。
     *
     * @param value 待判断字符串
     * @return 非空白时返回 true
     */
    public static boolean isNotBlank(String value) {
        return org.apache.commons.lang3.StringUtils.isNotBlank(value);
    }

    /**
     * 去除首尾空白；如果结果为空白，则返回 null。
     *
     * @param value 原始字符串
     * @return 标准化后的字符串或 null
     */
    public static String trimToNull(String value) {
        return org.apache.commons.lang3.StringUtils.trimToNull(value);
    }

    /**
     * 去除首尾空白；如果输入为 null，则返回空串。
     *
     * @param value 原始字符串
     * @return 非 null 字符串
     */
    public static String trimToEmpty(String value) {
        return org.apache.commons.lang3.StringUtils.trimToEmpty(value);
    }

    /**
     * 当输入为空白时返回默认值，否则返回去除首尾空白后的原值。
     *
     * @param value 原始字符串
     * @param defaultValue 默认值
     * @return 标准化后的字符串
     */
    public static String defaultIfBlank(String value, String defaultValue) {
        String normalized = trimToNull(value);
        return normalized == null ? defaultValue : normalized;
    }

    /**
     * 将 Windows 和旧 Mac 换行统一为 Unix 换行符 \n。
     *
     * @param value 原始字符串
     * @return 换行符统一后的字符串
     */
    public static String normalizeLineEndings(String value) {
        if (value == null) {
            return null;
        }
        return LINE_ENDING_PATTERN.matcher(value).replaceAll("\n");
    }

    /**
     * 将连续空白字符折叠为一个空格，并去除首尾空白。
     *
     * @param value 原始字符串
     * @return 折叠后的字符串；输入为 null 时返回 null
     */
    public static String collapseWhitespace(String value) {
        if (value == null) {
            return null;
        }
        String collapsed = MULTI_WHITESPACE_PATTERN.matcher(value).replaceAll(" ");
        return collapsed.trim();
    }

    /**
     * 按 Unicode 码点安全截断，避免把 emoji 等代理对字符截断成非法半个字符。
     *
     * @param value 原始字符串
     * @param maxCodePoints 最大码点数，必须非负
     * @return 截断后的字符串
     */
    public static String truncate(String value, int maxCodePoints) {
        if (value == null) {
            return null;
        }
        if (maxCodePoints < 0) {
            throw new IllegalArgumentException("maxCodePoints must not be negative");
        }
        if (value.codePointCount(0, value.length()) <= maxCodePoints) {
            return value;
        }
        int endIndex = value.offsetByCodePoints(0, maxCodePoints);
        return value.substring(0, endIndex);
    }

    /**
     * 按 Unicode 码点安全截断，并在发生截断时追加后缀。
     *
     * @param value 原始字符串
     * @param maxCodePoints 最大码点数，包含后缀在内
     * @param suffix 截断后缀，例如 "..."
     * @return 截断后的字符串
     */
    public static String truncateWithSuffix(String value, int maxCodePoints, String suffix) {
        if (value == null) {
            return null;
        }
        if (maxCodePoints < 0) {
            throw new IllegalArgumentException("maxCodePoints must not be negative");
        }
        String actualSuffix = suffix == null ? "" : suffix;
        int valueLength = value.codePointCount(0, value.length());
        if (valueLength <= maxCodePoints) {
            return value;
        }
        int suffixLength = actualSuffix.codePointCount(0, actualSuffix.length());
        if (suffixLength >= maxCodePoints) {
            return truncate(actualSuffix, maxCodePoints);
        }
        return truncate(value, maxCodePoints - suffixLength) + actualSuffix;
    }

    /**
     * 将驼峰命名转换为下划线小写命名。
     *
     * @param value 驼峰字符串
     * @return 下划线小写字符串
     */
    public static String camelToSnake(String value) {
        if (value == null) {
            return null;
        }
        StringBuilder builder = new StringBuilder(value.length() + 8);
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (Character.isUpperCase(ch)) {
                if (i > 0 && builder.length() > 0 && builder.charAt(builder.length() - 1) != '_') {
                    builder.append('_');
                }
                builder.append(Character.toLowerCase(ch));
            } else if (ch == '-' || ch == ' ') {
                builder.append('_');
            } else {
                builder.append(ch);
            }
        }
        return builder.toString().replaceAll("_+", "_");
    }

    /**
     * 将下划线或短横线命名转换为小驼峰命名。
     *
     * @param value 下划线或短横线字符串
     * @return 小驼峰字符串
     */
    public static String snakeToCamel(String value) {
        if (value == null) {
            return null;
        }
        String[] parts = value.toLowerCase(Locale.ROOT).split("[_-]+");
        if (parts.length == 0) {
            return value;
        }
        StringBuilder builder = new StringBuilder(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            if (!parts[i].isEmpty()) {
                builder.append(Character.toUpperCase(parts[i].charAt(0)));
                builder.append(parts[i].substring(1));
            }
        }
        return builder.toString();
    }

    /**
     * 按分隔符拆分字符串，自动去除空白项和空字符串。
     *
     * @param value 原始字符串
     * @param delimiter 分隔符
     * @return 不可修改列表
     */
    public static List<String> splitToList(String value, String delimiter) {
        if (isBlank(value)) {
            return Collections.emptyList();
        }
        Objects.requireNonNull(delimiter, "delimiter must not be null");
        return Arrays.stream(value.split(Pattern.quote(delimiter)))
                .map(StringUtils::trimToNull)
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * 使用分隔符拼接字符串，自动跳过 null 元素。
     *
     * @param values 字符串集合
     * @param delimiter 分隔符
     * @return 拼接后的字符串
     */
    public static String join(Iterable<String> values, String delimiter) {
        if (values == null) {
            return "";
        }
        return org.apache.commons.lang3.StringUtils.join(values, delimiter);
    }
}
