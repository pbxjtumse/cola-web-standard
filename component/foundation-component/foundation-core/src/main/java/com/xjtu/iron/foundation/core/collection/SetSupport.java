package com.xjtu.iron.foundation.core.collection;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 提供保序集合运算。
 */
public final class SetSupport {

    private SetSupport() {
    }

    public static <T> Set<T> union(Collection<? extends T> left, Collection<? extends T> right) {
        LinkedHashSet<T> result = new LinkedHashSet<>();
        if (left != null) {
            result.addAll(left);
        }
        if (right != null) {
            result.addAll(right);
        }
        return java.util.Collections.unmodifiableSet(result);
    }

    public static <T> Set<T> intersection(Collection<? extends T> left, Collection<? extends T> right) {
        if (left == null || right == null) {
            return Set.of();
        }
        LinkedHashSet<T> rightSet = new LinkedHashSet<>(right);
        LinkedHashSet<T> result = new LinkedHashSet<>();
        for (T value : left) {
            if (rightSet.contains(value)) {
                result.add(value);
            }
        }
        return java.util.Collections.unmodifiableSet(result);
    }
}
