package com.xjtu.iron.foundation.core.collection;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 两个集合按照某种业务或技术 key 比较后的差异结果。
 *
 * @param <T> 元素类型
 */
public final class CollectionDiff<T> {

    /** 新集合中新增的元素。 */
    private final List<T> added;

    /** 旧集合中被删除的元素。 */
    private final List<T> removed;

    /** 新旧集合中都存在的元素，使用新集合中的元素对象。 */
    private final List<T> retained;

    /**
     * 创建不可变集合差异对象。
     *
     * @param added 新增元素
     * @param removed 删除元素
     * @param retained 保留元素
     */
    public CollectionDiff(List<T> added, List<T> removed, List<T> retained) {
        this.added = List.copyOf(Objects.requireNonNull(added, "added must not be null"));
        this.removed = List.copyOf(Objects.requireNonNull(removed, "removed must not be null"));
        this.retained = List.copyOf(Objects.requireNonNull(retained, "retained must not be null"));
    }

    public List<T> getAdded() {
        return Collections.unmodifiableList(added);
    }

    public List<T> getRemoved() {
        return Collections.unmodifiableList(removed);
    }

    public List<T> getRetained() {
        return Collections.unmodifiableList(retained);
    }

    public boolean isEmpty() {
        return added.isEmpty() && removed.isEmpty();
    }
}
