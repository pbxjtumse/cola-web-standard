package com.xjtu.iron.foundation.core.collection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * List 专用工具门面。
 */
public final class ListUtils {

    private ListUtils() {
    }

    public static <T> List<T> emptyIfNull(List<T> values) {
        return values == null ? Collections.emptyList() : values;
    }

    public static <T> List<T> immutableCopy(List<T> values) {
        return values == null || values.isEmpty() ? Collections.emptyList() : List.copyOf(values);
    }

    public static <T> List<T> mutableCopy(List<T> values) {
        return values == null ? new ArrayList<>() : new ArrayList<>(values);
    }

    public static <T> Optional<T> first(List<T> values) {
        return values == null || values.isEmpty() ? Optional.empty() : Optional.ofNullable(values.get(0));
    }

    public static <T> Optional<T> last(List<T> values) {
        return values == null || values.isEmpty() ? Optional.empty() : Optional.ofNullable(values.get(values.size() - 1));
    }

    public static <T> Optional<T> get(List<T> values, int index) {
        if (values == null || index < 0 || index >= values.size()) {
            return Optional.empty();
        }
        return Optional.ofNullable(values.get(index));
    }

    public static <T> List<List<T>> partition(List<T> values, int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("size must be positive");
        }
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        List<List<T>> partitions = new ArrayList<>();
        for (List<T> part : org.apache.commons.collections4.ListUtils.partition(values, size)) {
            partitions.add(List.copyOf(part));
        }
        return Collections.unmodifiableList(partitions);
    }
}
