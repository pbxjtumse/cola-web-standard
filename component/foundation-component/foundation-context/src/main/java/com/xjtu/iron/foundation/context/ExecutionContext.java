package com.xjtu.iron.foundation.context;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 表示一次逻辑执行过程的不可变技术上下文。
 *
 * <p>该模型不绑定 ThreadLocal、MDC、Reactor 或消息 Header；传播方式由集成层决定。</p>
 */
public final class ExecutionContext {

    /** 全局可复用的空执行上下文。 */
    private static final ExecutionContext EMPTY = new ExecutionContext(Map.of());

    /** 按类型安全键保存的不可变上下文值。 */
    private final Map<ContextKey<?>, Object> values;

    ExecutionContext(Map<ContextKey<?>, Object> values) {
        this.values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    public static ExecutionContext empty() {
        return EMPTY;
    }

    public static ExecutionContextBuilder builder() {
        return new ExecutionContextBuilder();
    }

    public <T> Optional<T> get(ContextKey<T> key) {
        Object value = values.get(key);
        return Optional.ofNullable(key.cast(value));
    }

    public <T> T getOrDefault(ContextKey<T> key, T defaultValue) {
        return get(key).orElse(defaultValue);
    }

    public boolean contains(ContextKey<?> key) {
        return values.containsKey(key);
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    public int size() {
        return values.size();
    }

    public Map<ContextKey<?>, Object> asMap() {
        return values;
    }

    /**
     * 创建基于当前内容的构建器。
     */
    public ExecutionContextBuilder mutate() {
        return new ExecutionContextBuilder(values);
    }
}
