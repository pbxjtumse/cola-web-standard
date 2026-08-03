package com.xjtu.iron.foundation.reflection;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;

/**
 * 提供 java.lang.reflect.Type 分类和原始类型解析。
 */
public final class TypeSupport {

    private TypeSupport() {
    }

    public static Class<?> rawClass(Type type) {
        if (type instanceof Class<?> raw) {
            return raw;
        }
        if (type instanceof ParameterizedType parameterized) {
            return rawClass(parameterized.getRawType());
        }
        if (type instanceof GenericArrayType arrayType) {
            Class<?> component = rawClass(arrayType.getGenericComponentType());
            return java.lang.reflect.Array.newInstance(component, 0).getClass();
        }
        if (type instanceof WildcardType wildcard && wildcard.getUpperBounds().length > 0) {
            return rawClass(wildcard.getUpperBounds()[0]);
        }
        throw new ReflectionException("cannot resolve raw class from type: " + type);
    }

    public static boolean isParameterized(Type type) {
        return type instanceof ParameterizedType;
    }
}
