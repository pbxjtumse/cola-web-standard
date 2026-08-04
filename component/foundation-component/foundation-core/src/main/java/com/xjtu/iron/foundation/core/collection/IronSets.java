package com.xjtu.iron.foundation.core.collection;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Set 工具统一门面。
 */
public final class IronSets {

    private IronSets() {
    }

    public static <T> Set<T> immutableCopy(Collection<T> values) {
        return values == null || values.isEmpty() ? Collections.emptySet() : Set.copyOf(values);
    }

    public static <T> Set<T> linkedHashSet(Collection<T> values) {
        return values == null ? new LinkedHashSet<>() : new LinkedHashSet<>(values);
    }

    public static <T> Set<T> union(Collection<T> left, Collection<T> right) {
        Set<T> result = linkedHashSet(left);
        if (right != null) {
            result.addAll(right);
        }
        return Collections.unmodifiableSet(result);
    }

    public static <T> Set<T> intersection(Collection<T> left, Collection<T> right) {
        if (left == null || right == null) {
            return Collections.emptySet();
        }
        Set<T> result = linkedHashSet(left);
        result.retainAll(right);
        return Collections.unmodifiableSet(result);
    }

    public static <T> Set<T> subtract(Collection<T> left, Collection<T> right) {
        if (left == null) {
            return Collections.emptySet();
        }
        Set<T> result = linkedHashSet(left);
        if (right != null) {
            result.removeAll(right);
        }
        return Collections.unmodifiableSet(result);
    }
}
