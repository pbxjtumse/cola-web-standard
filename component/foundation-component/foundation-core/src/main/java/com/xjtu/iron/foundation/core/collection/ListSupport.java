package com.xjtu.iron.foundation.core.collection;

import java.util.List;
import java.util.Optional;

/**
 * 提供列表安全访问能力。
 */
public final class ListSupport {

    private ListSupport() {
    }

    public static <T> Optional<T> first(List<? extends T> values) {
        return values == null || values.isEmpty() ? Optional.empty() : Optional.ofNullable(values.get(0));
    }

    public static <T> Optional<T> last(List<? extends T> values) {
        return values == null || values.isEmpty()
                ? Optional.empty()
                : Optional.ofNullable(values.get(values.size() - 1));
    }

    public static <T> Optional<T> get(List<? extends T> values, int index) {
        if (values == null || index < 0 || index >= values.size()) {
            return Optional.empty();
        }
        return Optional.ofNullable(values.get(index));
    }
}
