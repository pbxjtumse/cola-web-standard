package com.xjtu.iron.foundation.core.text;

import java.util.Collection;
import java.util.StringJoiner;
import java.util.function.Function;

/**
 * 提供集合文本安全拼接能力。
 */
public final class TextJoinerSupport {

    private TextJoinerSupport() {
    }

    /**
     * 将集合元素映射为文本后拼接，自动忽略 {@code null} 结果。
     */
    public static <T> String join(Collection<T> values, String delimiter, Function<T, String> mapper) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        if (delimiter == null || mapper == null) {
            throw new IllegalArgumentException("delimiter and mapper must not be null");
        }

        StringJoiner joiner = new StringJoiner(delimiter);
        for (T value : values) {
            String mapped = mapper.apply(value);
            if (mapped != null) {
                joiner.add(mapped);
            }
        }
        return joiner.toString();
    }
}
