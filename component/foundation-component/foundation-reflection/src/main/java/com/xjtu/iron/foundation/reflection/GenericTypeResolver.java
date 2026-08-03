package com.xjtu.iron.foundation.reflection;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Optional;

/**
 * 解析类实现接口时声明的泛型实参。
 */
public final class GenericTypeResolver {

    private GenericTypeResolver() {
    }

    public static Optional<Type> resolveInterfaceArgument(Class<?> implementation,
                                                           Class<?> targetInterface,
                                                           int argumentIndex) {
        if (implementation == null || targetInterface == null || argumentIndex < 0) {
            return Optional.empty();
        }
        for (Class<?> current : ClassSupport.hierarchy(implementation)) {
            for (Type candidate : current.getGenericInterfaces()) {
                Optional<Type> resolved = inspect(candidate, targetInterface, argumentIndex);
                if (resolved.isPresent()) {
                    return resolved;
                }
            }
        }
        return Optional.empty();
    }

    private static Optional<Type> inspect(Type candidate, Class<?> targetInterface, int argumentIndex) {
        if (!(candidate instanceof ParameterizedType parameterized)) {
            return Optional.empty();
        }
        if (!targetInterface.equals(TypeSupport.rawClass(parameterized.getRawType()))) {
            return Optional.empty();
        }
        Type[] arguments = parameterized.getActualTypeArguments();
        return argumentIndex < arguments.length ? Optional.of(arguments[argumentIndex]) : Optional.empty();
    }
}
