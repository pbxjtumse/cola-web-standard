package com.xjtu.iron.foundation.context;

import java.util.Objects;

/**
 * 描述具有名称和运行时类型的上下文键。
 *
 * @param <T> 上下文值类型
 */
public final class ContextKey<T> {

    /** 跨组件稳定使用的上下文键名称。 */
    private final String name;
    /** 上下文值的运行时类型。 */
    private final Class<T> valueType;

    private ContextKey(String name, Class<T> valueType) {
        this.name = name;
        this.valueType = valueType;
    }

    public static <T> ContextKey<T> of(String name, Class<T> valueType) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("context key name must not be blank");
        }
        return new ContextKey<>(name, Objects.requireNonNull(valueType, "valueType must not be null"));
    }

    public String getName() { return name; }
    public Class<T> getValueType() { return valueType; }

    /**
     * 校验并转换外部值。
     */
    public T cast(Object value) {
        return value == null ? null : valueType.cast(value);
    }

    @Override
    public boolean equals(Object object) {
        return object instanceof ContextKey<?> other && name.equals(other.name) && valueType.equals(other.valueType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, valueType);
    }

    @Override
    public String toString() {
        return name + '<' + valueType.getSimpleName() + '>';
    }
}
