package com.xjtu.iron.foundation.reflection;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * 提供明确失败语义的构造器调用。
 */
public final class ConstructorSupport {

    private ConstructorSupport() {
    }

    public static <T> T newInstance(Class<T> type) {
        if (type == null) {
            throw new IllegalArgumentException("type must not be null");
        }
        try {
            Constructor<T> constructor = type.getDeclaredConstructor();
            if (!constructor.canAccess(null)) {
                constructor.trySetAccessible();
            }
            return constructor.newInstance();
        } catch (NoSuchMethodException exception) {
            throw new ReflectionException("type has no no-argument constructor: " + type.getName(), exception);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException exception) {
            throw new ReflectionException("failed to instantiate type: " + type.getName(), exception);
        }
    }
}
