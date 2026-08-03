package com.xjtu.iron.foundation.reflection;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;

/**
 * 提供方法查找和调用能力。
 */
public final class MethodSupport {

    private MethodSupport() {
    }

    public static Optional<Method> find(Class<?> type, String name, Class<?>... parameterTypes) {
        if (type == null || name == null) {
            return Optional.empty();
        }
        for (Class<?> current : ClassSupport.hierarchy(type)) {
            try {
                Method method = current.getDeclaredMethod(name, parameterTypes);
                method.trySetAccessible();
                return Optional.of(method);
            } catch (NoSuchMethodException ignored) {
                // 当前层级不存在时继续向父类查找。
            }
        }
        return Optional.empty();
    }

    public static Object invoke(Method method, Object target, Object... arguments) {
        if (method == null) {
            throw new IllegalArgumentException("method must not be null");
        }
        try {
            return method.invoke(target, arguments);
        } catch (IllegalAccessException | InvocationTargetException exception) {
            throw new ReflectionException("failed to invoke method: " + method, exception);
        }
    }
}
