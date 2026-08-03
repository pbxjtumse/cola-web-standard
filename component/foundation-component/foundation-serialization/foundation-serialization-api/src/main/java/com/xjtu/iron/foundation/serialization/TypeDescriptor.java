package com.xjtu.iron.foundation.serialization;

import com.xjtu.iron.foundation.reflection.GenericType;
import com.xjtu.iron.foundation.reflection.TypeSupport;

import java.lang.reflect.Type;
import java.util.Objects;

/**
 * 描述反序列化目标类型，并支持泛型类型。
 */
public final class TypeDescriptor<T> {

    /** 反序列化目标的完整 Java Type。 */
    private final Type type;

    private TypeDescriptor(Type type) {
        this.type = type;
    }

    public static <T> TypeDescriptor<T> of(Class<T> type) {
        return new TypeDescriptor<>(Objects.requireNonNull(type, "type must not be null"));
    }

    public static <T> TypeDescriptor<T> of(GenericType<T> genericType) {
        return new TypeDescriptor<>(Objects.requireNonNull(genericType, "genericType must not be null").getType());
    }

    public static <T> TypeDescriptor<T> of(Type type) {
        return new TypeDescriptor<>(Objects.requireNonNull(type, "type must not be null"));
    }

    public Type getType() { return type; }
    public Class<?> getRawClass() { return TypeSupport.rawClass(type); }

    @Override
    public String toString() {
        return type.getTypeName();
    }
}
