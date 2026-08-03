package com.xjtu.iron.foundation.core.collection;

import com.xjtu.iron.foundation.core.text.StringUtils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Map 工具统一门面。
 */
public final class MapUtils {

    private MapUtils() {
    }

    public static boolean isEmpty(Map<?, ?> value) {
        return value == null || value.isEmpty();
    }

    public static boolean isNotEmpty(Map<?, ?> value) {
        return !isEmpty(value);
    }

    public static <K, V> Map<K, V> emptyIfNull(Map<K, V> value) {
        return value == null ? Collections.emptyMap() : value;
    }

    public static <K, V> Map<K, V> immutableCopy(Map<K, V> value) {
        return value == null || value.isEmpty() ? Collections.emptyMap() : Map.copyOf(value);
    }

    public static <K, V> Map<K, V> mutableCopy(Map<K, V> value) {
        return value == null ? new LinkedHashMap<>() : new LinkedHashMap<>(value);
    }

    public static String getString(Map<String, ?> map, String key) {
        Object value = map == null ? null : map.get(key);
        return value == null ? null : String.valueOf(value);
    }

    public static Optional<Integer> getInteger(Map<String, ?> map, String key) {
        Object value = map == null ? null : map.get(key);
        if (value instanceof Integer integer) {
            return Optional.of(integer);
        }
        if (value instanceof Number number) {
            return Optional.of(number.intValue());
        }
        try {
            return StringUtils.isBlank(String.valueOf(value)) ? Optional.empty() : Optional.of(Integer.parseInt(String.valueOf(value)));
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }

    public static <K, V> void putIfNotNull(Map<K, V> map, K key, V value) {
        Objects.requireNonNull(map, "map must not be null");
        if (key != null && value != null) {
            map.put(key, value);
        }
    }

    public static <K, V> Map<K, V> merge(Map<K, V> first, Map<K, V> second) {
        Map<K, V> result = mutableCopy(first);
        if (second != null) {
            result.putAll(second);
        }
        return Collections.unmodifiableMap(result);
    }
}
