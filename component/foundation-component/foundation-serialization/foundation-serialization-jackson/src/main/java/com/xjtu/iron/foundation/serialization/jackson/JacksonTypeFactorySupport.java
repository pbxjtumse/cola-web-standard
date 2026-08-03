package com.xjtu.iron.foundation.serialization.jackson;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.xjtu.iron.foundation.serialization.TypeDescriptor;

/**
 * 将 Foundation 类型描述转换为 Jackson JavaType。
 */
public final class JacksonTypeFactorySupport {

    private JacksonTypeFactorySupport() {
    }

    public static JavaType toJavaType(TypeFactory typeFactory, TypeDescriptor<?> descriptor) {
        if (typeFactory == null || descriptor == null) {
            throw new IllegalArgumentException("typeFactory and descriptor must not be null");
        }
        return typeFactory.constructType(descriptor.getType());
    }
}
