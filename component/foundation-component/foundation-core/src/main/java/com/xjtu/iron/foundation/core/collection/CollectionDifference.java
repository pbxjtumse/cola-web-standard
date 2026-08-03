package com.xjtu.iron.foundation.core.collection;

import java.util.List;

/**
 * 描述两个集合之间的新增、删除和保留元素。
 */
public final class CollectionDifference<T> {

    /** 新集合中新增的元素。 */
    private final List<T> added;
    /** 旧集合中被删除的元素。 */
    private final List<T> removed;
    /** 两个集合中均保留的元素。 */
    private final List<T> retained;

    public CollectionDifference(List<T> added, List<T> removed, List<T> retained) {
        this.added = List.copyOf(added);
        this.removed = List.copyOf(removed);
        this.retained = List.copyOf(retained);
    }

    public List<T> getAdded() {
        return added;
    }

    public List<T> getRemoved() {
        return removed;
    }

    public List<T> getRetained() {
        return retained;
    }

    /**
     * 判断两个集合是否不存在成员差异。
     */
    public boolean isUnchanged() {
        return added.isEmpty() && removed.isEmpty();
    }
}
