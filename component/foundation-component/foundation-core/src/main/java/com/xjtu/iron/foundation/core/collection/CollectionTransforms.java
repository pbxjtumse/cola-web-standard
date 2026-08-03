package com.xjtu.iron.foundation.core.collection;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * 提供集合元素映射和扁平化能力。
 */
public final class CollectionTransforms {

    private CollectionTransforms() {
    }

    public static <T, R> List<R> map(Collection<? extends T> values, Function<? super T, ? extends R> mapper) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        Objects.requireNonNull(mapper, "mapper must not be null");
        java.util.ArrayList<R> result = new java.util.ArrayList<>();
        for (T value : values) {
            result.add(mapper.apply(value));
        }
        return List.copyOf(result);
    }

    public static <T, R> List<R> flatMap(Collection<? extends T> values,
                                          Function<? super T, ? extends Collection<? extends R>> mapper) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        Objects.requireNonNull(mapper, "mapper must not be null");
        java.util.ArrayList<R> result = new java.util.ArrayList<>();
        for (T value : values) {
            Collection<? extends R> mapped = mapper.apply(value);
            if (mapped != null) {
                result.addAll(mapped);
            }
        }
        return List.copyOf(result);
    }
}
