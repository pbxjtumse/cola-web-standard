package com.xjtu.iron.foundation.context;

import java.util.Objects;

/**
 * 表示一个类型安全的上下文条目。
 */
public final class ContextEntry<T> {

    /** 条目对应的类型安全上下文键。 */
    private final ContextKey<T> key;
    /** 经过键类型校验后的上下文值。 */
    private final T value;

    public ContextEntry(ContextKey<T> key, T value) {
        this.key = Objects.requireNonNull(key, "key must not be null");
        this.value = key.cast(value);
    }

    public ContextKey<T> getKey() { return key; }
    public T getValue() { return value; }
}
