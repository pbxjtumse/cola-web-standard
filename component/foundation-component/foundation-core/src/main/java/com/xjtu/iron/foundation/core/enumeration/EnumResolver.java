package com.xjtu.iron.foundation.core.enumeration;

import java.util.Optional;

/**
 * 提供按名称和稳定编码解析枚举的能力。
 */
public final class EnumResolver {

    private EnumResolver() {
    }

    public static <E extends Enum<E>> Optional<E> byName(Class<E> enumType,
                                                          String name,
                                                          boolean ignoreCase) {
        if (enumType == null || name == null) {
            return Optional.empty();
        }
        for (E value : enumType.getEnumConstants()) {
            if (ignoreCase ? value.name().equalsIgnoreCase(name) : value.name().equals(name)) {
                return Optional.of(value);
            }
        }
        return Optional.empty();
    }

    public static <C, E extends Enum<E> & CodeEnum<C>> Optional<E> byCode(Class<E> enumType, C code) {
        if (enumType == null || code == null) {
            return Optional.empty();
        }
        for (E value : enumType.getEnumConstants()) {
            if (java.util.Objects.equals(value.getCode(), code)) {
                return Optional.of(value);
            }
        }
        return Optional.empty();
    }

    public static <E extends Enum<E>> E requireByName(Class<E> enumType, String name, boolean ignoreCase) {
        return byName(enumType, name, ignoreCase)
                .orElseThrow(() -> new IllegalArgumentException("unknown enum name: " + name));
    }
}
