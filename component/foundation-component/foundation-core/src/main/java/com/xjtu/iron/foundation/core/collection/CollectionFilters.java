package com.xjtu.iron.foundation.core.collection;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * 提供返回不可变结果的集合过滤能力。
 */
public final class CollectionFilters {

    private CollectionFilters() {
    }

    public static <T> List<T> nonNull(Collection<? extends T> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        java.util.ArrayList<T> result = new java.util.ArrayList<>();
        for (T value : values) {
            if (value != null) {
                result.add(value);
            }
        }
        return List.copyOf(result);
    }

    public static <T> List<T> filter(Collection<? extends T> values, Predicate<? super T> predicate) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        Objects.requireNonNull(predicate, "predicate must not be null");
        java.util.ArrayList<T> result = new java.util.ArrayList<>();
        for (T value : values) {
            if (predicate.test(value)) {
                result.add(value);
            }
        }
        return List.copyOf(result);
    }
}
