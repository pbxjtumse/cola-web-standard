package com.xjtu.iron.foundation.reflection;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 提供字段查找和安全读取能力。
 */
public final class FieldSupport {

    private FieldSupport() {
    }

    public static List<Field> allFields(Class<?> type) {
        if (type == null) {
            return List.of();
        }
        List<Field> result = new ArrayList<>();
        for (Class<?> current : ClassSupport.hierarchy(type)) {
            for (Field field : current.getDeclaredFields()) {
                field.trySetAccessible();
                result.add(field);
            }
        }
        return List.copyOf(result);
    }

    public static Optional<Field> find(Class<?> type, String name) {
        return allFields(type).stream().filter(field -> field.getName().equals(name)).findFirst();
    }

    public static Object read(Field field, Object target) {
        try {
            return field.get(target);
        } catch (IllegalAccessException exception) {
            throw new ReflectionException("failed to read field: " + field, exception);
        }
    }
}
