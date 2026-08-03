package com.xjtu.iron.foundation.reflection;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * 提供基于 JavaBeans 规范的属性描述和读取。
 *
 * <p>该类不提供 Bean Copy，避免隐藏字段映射和空值覆盖语义。</p>
 */
public final class PropertySupport {

    private PropertySupport() {
    }

    public static List<PropertyDescriptor> descriptors(Class<?> type) {
        if (type == null) {
            return List.of();
        }
        try {
            return Arrays.stream(Introspector.getBeanInfo(type, Object.class).getPropertyDescriptors())
                    .map(descriptor -> new PropertyDescriptor(
                            descriptor.getName(),
                            descriptor.getPropertyType(),
                            descriptor.getReadMethod(),
                            descriptor.getWriteMethod()
                    ))
                    .toList();
        } catch (IntrospectionException exception) {
            throw new ReflectionException("failed to inspect bean properties: " + type.getName(), exception);
        }
    }

    public static Optional<PropertyDescriptor> find(Class<?> type, String name) {
        return descriptors(type).stream().filter(property -> property.getName().equals(name)).findFirst();
    }

    public static Object read(Object bean, String propertyName) {
        if (bean == null) {
            return null;
        }
        PropertyDescriptor descriptor = find(bean.getClass(), propertyName)
                .orElseThrow(() -> new ReflectionException("unknown property: " + propertyName));
        Method readMethod = descriptor.getReadMethod();
        if (readMethod == null) {
            throw new ReflectionException("property is not readable: " + propertyName);
        }
        return MethodSupport.invoke(readMethod, bean);
    }
}
