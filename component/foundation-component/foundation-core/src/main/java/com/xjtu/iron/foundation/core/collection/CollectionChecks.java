package com.xjtu.iron.foundation.core.collection;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;

import java.util.Collection;
import java.util.Map;

/**
 * 提供集合和映射的空值检查。
 */
public final class CollectionChecks {

    private CollectionChecks() {
    }

    public static boolean isEmpty(Collection<?> values) {
        return CollectionUtils.isEmpty(values);
    }

    public static boolean isNotEmpty(Collection<?> values) {
        return !isEmpty(values);
    }

    public static boolean isEmpty(Map<?, ?> values) {
        return MapUtils.isEmpty(values);
    }

    public static boolean isNotEmpty(Map<?, ?> values) {
        return !isEmpty(values);
    }
}
