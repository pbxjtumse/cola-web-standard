package com.xjtu.iron.foundation.core.text;

import java.util.Map;
import java.util.Objects;

/**
 * 解析形如 {@code ${name}} 的简单占位符。
 *
 * <p>该实现不执行表达式，也不访问系统属性，适合配置快照和技术日志模板。</p>
 */
public final class PlaceholderResolver {

    /** 占位符开始标记。 */
    private static final String PREFIX = "${";
    /** 占位符结束标记。 */
    private static final char SUFFIX = '}';

    private PlaceholderResolver() {
    }

    /**
     * 使用给定变量替换模板中的占位符。
     */
    public static String resolve(String template, Map<String, ?> variables, boolean ignoreUnresolved) {
        if (template == null) {
            return null;
        }
        Objects.requireNonNull(variables, "variables must not be null");

        StringBuilder result = new StringBuilder(template.length());
        int cursor = 0;
        while (cursor < template.length()) {
            int start = template.indexOf(PREFIX, cursor);
            if (start < 0) {
                result.append(template, cursor, template.length());
                break;
            }
            result.append(template, cursor, start);
            int end = template.indexOf(SUFFIX, start + PREFIX.length());
            if (end < 0) {
                throw new IllegalArgumentException("Unclosed placeholder at index " + start);
            }

            String name = template.substring(start + PREFIX.length(), end);
            Object value = variables.get(name);
            if (value == null) {
                if (ignoreUnresolved) {
                    result.append(template, start, end + 1);
                } else {
                    throw new IllegalArgumentException("Unresolved placeholder: " + name);
                }
            } else {
                result.append(value);
            }
            cursor = end + 1;
        }
        return result.toString();
    }
}
