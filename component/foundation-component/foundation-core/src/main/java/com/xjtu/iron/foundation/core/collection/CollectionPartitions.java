package com.xjtu.iron.foundation.core.collection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 将集合拆分为独立且不可修改的批次。
 */
public final class CollectionPartitions {

    private CollectionPartitions() {
    }

    /**
     * 按批次大小拆分列表。
     *
     * <p>返回值不是原列表的视图，原列表后续修改不会污染已生成批次。</p>
     */
    public static <T> List<List<T>> partition(List<? extends T> source, int batchSize) {
        if (batchSize <= 0) {
            throw new IllegalArgumentException("batchSize must be positive");
        }
        if (source == null || source.isEmpty()) {
            return List.of();
        }

        List<List<T>> result = new ArrayList<>((source.size() + batchSize - 1) / batchSize);
        for (int start = 0; start < source.size(); start += batchSize) {
            int end = Math.min(start + batchSize, source.size());
            result.add(List.copyOf(source.subList(start, end)));
        }
        return Collections.unmodifiableList(result);
    }
}
