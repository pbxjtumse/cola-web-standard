package com.xjtu.iron.foundation.reflection;

import java.lang.reflect.Method;
import java.util.Objects;

/**
 * 描述可读写 JavaBean 属性。
 */
public final class PropertyDescriptor {

    /** JavaBean 属性名称。 */
    private final String name;
    /** 属性声明的运行时类型。 */
    private final Class<?> propertyType;
    /** 属性读取方法；不可读时为空。 */
    private final Method readMethod;
    /** 属性写入方法；不可写时为空。 */
    private final Method writeMethod;

    public PropertyDescriptor(String name, Class<?> propertyType, Method readMethod, Method writeMethod) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.propertyType = Objects.requireNonNull(propertyType, "propertyType must not be null");
        this.readMethod = readMethod;
        this.writeMethod = writeMethod;
    }

    public String getName() { return name; }
    public Class<?> getPropertyType() { return propertyType; }
    public Method getReadMethod() { return readMethod; }
    public Method getWriteMethod() { return writeMethod; }
    public boolean isReadable() { return readMethod != null; }
    public boolean isWritable() { return writeMethod != null; }
}
