package com.xjtu.iron.foundation.core.collection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 集合工具统一门面。
 *
 * <p>该类优先复用 JDK 与 Apache Commons Collections，Foundation 只补充项目内常见且需要
 * 统一语义的少量方法。不要把所有集合操作都塞进这里；当方法数明显膨胀时，优先考虑是否应该直接
 * 使用 JDK Stream 或 Commons Collections。</p>
 */
public final class CollectionUtils {

    private CollectionUtils() {
    }

    public static boolean isEmpty(Collection<?> values) {
        return org.apache.commons.collections4.CollectionUtils.isEmpty(values);
    }

    public static boolean isNotEmpty(Collection<?> values) {
        return org.apache.commons.collections4.CollectionUtils.isNotEmpty(values);
    }

    public static <T> Collection<T> emptyIfNull(Collection<T> values) {
        return values == null ? Collections.emptyList() : values;
    }

    public static <T> List<T> immutableCopy(Collection<T> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        return List.copyOf(values);
    }

    public static <T> List<T> mutableCopy(Collection<T> values) {
        return values == null ? new ArrayList<>() : new ArrayList<>(values);
    }

    public static <T> Optional<T> first(Collection<T> values) {
        if (values == null || values.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(values.iterator().next());
    }

    public static <T> List<T> filterNotNull(Collection<T> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        return values.stream().filter(Objects::nonNull).collect(Collectors.toUnmodifiableList());
    }

    public static <T, R> List<R> map(Collection<T> values, Function<? super T, ? extends R> mapper) {
        Objects.requireNonNull(mapper, "mapper must not be null");
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        return values.stream().map(mapper).collect(Collectors.toUnmodifiableList());
    }

    public static <T> List<T> filter(Collection<T> values, Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate must not be null");
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        return values.stream().filter(predicate).collect(Collectors.toUnmodifiableList());
    }

    public static <T, K> Map<K, T> toMap(Collection<T> values, Function<? super T, ? extends K> keyMapper) {
        Objects.requireNonNull(keyMapper, "keyMapper must not be null");
        if (values == null || values.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<K, T> result = new LinkedHashMap<>();
        for (T value : values) {
            K key = keyMapper.apply(value);
            if (key != null && !result.containsKey(key)) {
                result.put(key, value);
            }
        }
        return Collections.unmodifiableMap(result);
    }

    public static <T, K> Map<K, List<T>> groupBy(Collection<T> values, Function<? super T, ? extends K> keyMapper) {
        Objects.requireNonNull(keyMapper, "keyMapper must not be null");
        if (values == null || values.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<K, List<T>> grouped = values.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(keyMapper, LinkedHashMap::new, Collectors.toList()));
        Map<K, List<T>> result = new LinkedHashMap<>();
        grouped.forEach((key, list) -> result.put(key, List.copyOf(list)));
        return Collections.unmodifiableMap(result);
    }

    public static <T, K> List<T> distinctBy(Collection<T> values, Function<? super T, ? extends K> keyMapper) {
        Objects.requireNonNull(keyMapper, "keyMapper must not be null");
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        Set<K> seen = new LinkedHashSet<>();
        List<T> result = new ArrayList<>();
        for (T value : values) {
            K key = keyMapper.apply(value);
            if (seen.add(key)) {
                result.add(value);
            }
        }
        return Collections.unmodifiableList(result);
    }

    public static <T, K> CollectionDiff<T> difference(Collection<T> oldValues,
                                                      Collection<T> newValues,
                                                      Function<? super T, ? extends K> keyMapper) {
        Objects.requireNonNull(keyMapper, "keyMapper must not be null");
        Map<K, T> oldMap = toMap(emptyIfNull(oldValues), keyMapper);
        Map<K, T> newMap = toMap(emptyIfNull(newValues), keyMapper);
        List<T> added = new ArrayList<>();
        List<T> removed = new ArrayList<>();
        List<T> retained = new ArrayList<>();
        for (Map.Entry<K, T> entry : newMap.entrySet()) {
            if (oldMap.containsKey(entry.getKey())) {
                retained.add(entry.getValue());
            } else {
                added.add(entry.getValue());
            }
        }
        for (Map.Entry<K, T> entry : oldMap.entrySet()) {
            if (!newMap.containsKey(entry.getKey())) {
                removed.add(entry.getValue());
            }
        }
        return new CollectionDiff<>(added, removed, retained);
    }
}
