package com.xjtu.iron.foundation.reflection;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * 通过匿名子类保留泛型类型信息。
 *
 * <pre>{@code
 * GenericType<List<String>> type = new GenericType<>() {};
 * }</pre>
 */
public abstract class GenericType<T> {

    /** 匿名子类捕获到的完整泛型类型。 */
    private final Type type;

    protected GenericType() {
        Type parent = getClass().getGenericSuperclass();
        if (!(parent instanceof ParameterizedType parameterized)) {
            throw new IllegalStateException("GenericType must be created as an anonymous parameterized subclass");
        }
        this.type = parameterized.getActualTypeArguments()[0];
    }

    public Type getType() {
        return type;
    }

    public Class<?> getRawClass() {
        return TypeSupport.rawClass(type);
    }
}
