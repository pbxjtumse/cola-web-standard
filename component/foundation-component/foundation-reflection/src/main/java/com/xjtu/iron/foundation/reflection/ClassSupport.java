package com.xjtu.iron.foundation.reflection;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 提供类型加载和继承层次检查。
 */
public final class ClassSupport {

    private ClassSupport() {
    }

    public static Optional<Class<?>> load(String className, ClassLoader classLoader) {
        if (className == null || className.isBlank()) {
            return Optional.empty();
        }
        ClassLoader actual = classLoader == null ? Thread.currentThread().getContextClassLoader() : classLoader;
        try {
            return Optional.of(Class.forName(className, false, actual));
        } catch (ClassNotFoundException exception) {
            return Optional.empty();
        }
    }

    /**
     * 返回从当前类型到 Object 之前的继承链。
     */
    public static List<Class<?>> hierarchy(Class<?> type) {
        if (type == null) {
            return List.of();
        }
        List<Class<?>> result = new ArrayList<>();
        Class<?> current = type;
        while (current != null && current != Object.class) {
            result.add(current);
            current = current.getSuperclass();
        }
        return List.copyOf(result);
    }

    public static boolean isConcrete(Class<?> type) {
        if (type == null) {
            return false;
        }
        int modifiers = type.getModifiers();
        return !type.isInterface() && !java.lang.reflect.Modifier.isAbstract(modifiers);
    }
}
