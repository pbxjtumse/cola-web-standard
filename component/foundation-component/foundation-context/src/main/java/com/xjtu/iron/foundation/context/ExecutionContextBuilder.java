package com.xjtu.iron.foundation.context;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * ExecutionContext 构建器。
 */
public final class ExecutionContextBuilder {

    private final Map<ContextKey<?>, Object> values = new LinkedHashMap<>();

    public <T> ExecutionContextBuilder put(ContextKey<T> key, T value) {
        Objects.requireNonNull(key, "key must not be null");
        if (value != null) {
            values.put(key, key.getType().cast(value));
        }
        return this;
    }

    ExecutionContextBuilder putRaw(ContextKey<?> key, Object value) {
        values.put(key, value);
        return this;
    }

    public ExecutionContext build() {
        return new ExecutionContext(values);
    }
}
