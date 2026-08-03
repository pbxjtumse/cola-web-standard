package com.xjtu.iron.foundation.context;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 不可变执行上下文。
 *
 * <p>该模型只保存上下文数据，不决定数据存放在 ThreadLocal、MDC、Reactor Context 还是消息 Header 中。
 * 具体传播由并行、消息或可观测集成模块完成。</p>
 */
public final class ExecutionContext {

    private final Map<ContextKey<?>, Object> values;

    ExecutionContext(Map<ContextKey<?>, Object> values) {
        this.values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    public static ExecutionContext empty() {
        return new ExecutionContext(Collections.emptyMap());
    }

    public static ExecutionContextBuilder builder() {
        return new ExecutionContextBuilder();
    }

    public <T> Optional<T> get(ContextKey<T> key) {
        Object value = values.get(key);
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(key.getType().cast(value));
    }

    public Map<ContextKey<?>, Object> asMap() {
        return values;
    }

    public ExecutionContextBuilder toBuilder() {
        ExecutionContextBuilder builder = builder();
        values.forEach(builder::putRaw);
        return builder;
    }
}
