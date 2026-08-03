package com.xjtu.iron.foundation.core.collection;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 创建保持顺序的防御性不可变副本。
 */
public final class CollectionCopies {

    private CollectionCopies() {
    }

    public static <T> List<T> immutableList(Iterable<? extends T> source) {
        if (source == null) {
            return List.of();
        }
        List<T> result = new ArrayList<>();
        source.forEach(result::add);
        return List.copyOf(result);
    }

    public static <T> Set<T> immutableLinkedSet(Iterable<? extends T> source) {
        if (source == null) {
            return Set.of();
        }
        LinkedHashSet<T> result = new LinkedHashSet<>();
        source.forEach(result::add);
        return java.util.Collections.unmodifiableSet(result);
    }

    public static <K, V> Map<K, V> immutableLinkedMap(Map<? extends K, ? extends V> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        return java.util.Collections.unmodifiableMap(new LinkedHashMap<>(source));
    }
}
