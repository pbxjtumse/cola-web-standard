package com.xjtu.iron.foundation.core.collection;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BinaryOperator;

/**
 * 提供映射合并和反转能力。
 */
public final class MapSupport {

    private MapSupport() {
    }

    public static <K, V> Map<K, V> merge(Map<? extends K, ? extends V> left,
                                          Map<? extends K, ? extends V> right,
                                          BinaryOperator<V> conflictResolver) {
        if (conflictResolver == null) {
            throw new IllegalArgumentException("conflictResolver must not be null");
        }
        LinkedHashMap<K, V> result = new LinkedHashMap<>();
        if (left != null) {
            result.putAll(left);
        }
        if (right != null) {
            right.forEach((key, value) -> result.merge(key, value, conflictResolver));
        }
        return java.util.Collections.unmodifiableMap(result);
    }

    /**
     * 反转键值关系；遇到重复值时拒绝覆盖。
     */
    public static <K, V> Map<V, K> invertUnique(Map<? extends K, ? extends V> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        LinkedHashMap<V, K> result = new LinkedHashMap<>();
        source.forEach((key, value) -> {
            if (result.putIfAbsent(value, key) != null) {
                throw new IllegalArgumentException("duplicate map value: " + value);
            }
        });
        return java.util.Collections.unmodifiableMap(result);
    }
}
