package com.xjtu.iron.foundation.core.collection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 计算集合成员差异，并保持输入顺序。
 */
public final class CollectionDiff {

    private CollectionDiff() {
    }

    public static <T> CollectionDifference<T> compare(Collection<? extends T> before,
                                                        Collection<? extends T> after) {
        List<T> left = before == null ? List.of() : new ArrayList<>(before);
        List<T> right = after == null ? List.of() : new ArrayList<>(after);
        Set<T> leftSet = new LinkedHashSet<>(left);
        Set<T> rightSet = new LinkedHashSet<>(right);

        List<T> added = right.stream().filter(value -> !leftSet.contains(value)).toList();
        List<T> removed = left.stream().filter(value -> !rightSet.contains(value)).toList();
        List<T> retained = right.stream().filter(leftSet::contains).toList();
        return new CollectionDifference<>(added, removed, retained);
    }
}
