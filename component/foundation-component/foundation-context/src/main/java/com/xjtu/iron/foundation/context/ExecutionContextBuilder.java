package com.xjtu.iron.foundation.context;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 构建不可变执行上下文。
 */
public final class ExecutionContextBuilder {

    /** 构建过程中暂存的上下文值。 */
    private final Map<ContextKey<?>, Object> values;

    public ExecutionContextBuilder() {
        this.values = new LinkedHashMap<>();
    }

    ExecutionContextBuilder(Map<ContextKey<?>, Object> source) {
        this.values = new LinkedHashMap<>(source);
    }

    public <T> ExecutionContextBuilder put(ContextKey<T> key, T value) {
        if (key == null) {
            throw new IllegalArgumentException("key must not be null");
        }
        if (value == null) {
            values.remove(key);
        } else {
            values.put(key, key.cast(value));
        }
        return this;
    }

    public ExecutionContextBuilder remove(ContextKey<?> key) {
        values.remove(key);
        return this;
    }

    public ExecutionContextBuilder putAll(ExecutionContext context) {
        if (context != null) {
            values.putAll(context.asMap());
        }
        return this;
    }

    public ExecutionContext build() {
        return values.isEmpty() ? ExecutionContext.empty() : new ExecutionContext(values);
    }
}
