package com.xjtu.iron.foundation.core.collection;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 将对象集合建立为保序索引。
 */
public final class CollectionIndexers {

    private CollectionIndexers() {
    }

    /**
     * 按唯一键建立索引；遇到重复键时抛出异常，避免静默覆盖数据。
     */
    public static <K, V> Map<K, V> uniqueIndex(List<? extends V> values, Function<? super V, ? extends K> keyMapper) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        if (keyMapper == null) {
            throw new IllegalArgumentException("keyMapper must not be null");
        }

        Map<K, V> result = new LinkedHashMap<>();
        for (V value : values) {
            K key = keyMapper.apply(value);
            if (result.putIfAbsent(key, value) != null) {
                throw new IllegalArgumentException("duplicate collection key: " + key);
            }
        }
        return java.util.Collections.unmodifiableMap(result);
    }

    /**
     * 按键分组，同时保留原集合顺序。
     */
    public static <K, V> Map<K, List<V>> groupBy(List<? extends V> values, Function<? super V, ? extends K> keyMapper) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        Map<K, java.util.ArrayList<V>> mutable = new LinkedHashMap<>();
        for (V value : values) {
            K key = keyMapper.apply(value);
            mutable.computeIfAbsent(key, ignored -> new java.util.ArrayList<>()).add(value);
        }
        Map<K, List<V>> result = new LinkedHashMap<>();
        mutable.forEach((key, grouped) -> result.put(key, List.copyOf(grouped)));
        return java.util.Collections.unmodifiableMap(result);
    }
}
