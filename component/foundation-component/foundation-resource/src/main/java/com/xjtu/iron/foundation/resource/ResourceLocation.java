package com.xjtu.iron.foundation.resource;

import java.util.Objects;

/**
 * 解析 classpath、file 和普通文件路径资源位置。
 */
public final class ResourceLocation {

    public static final String CLASSPATH_PREFIX = "classpath:";
    public static final String FILE_PREFIX = "file:";

    /** 原始且已去除首尾空白的资源位置。 */
    private final String value;

    private ResourceLocation(String value) {
        this.value = value;
    }

    public static ResourceLocation of(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("resource location must not be blank");
        }
        return new ResourceLocation(value.trim());
    }

    public boolean isClasspath() {
        return value.startsWith(CLASSPATH_PREFIX);
    }

    public boolean isFileUri() {
        return value.startsWith(FILE_PREFIX);
    }

    public String path() {
        if (isClasspath()) {
            return value.substring(CLASSPATH_PREFIX.length());
        }
        if (isFileUri()) {
            return value.substring(FILE_PREFIX.length());
        }
        return value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object object) {
        return object instanceof ResourceLocation other && value.equals(other.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
