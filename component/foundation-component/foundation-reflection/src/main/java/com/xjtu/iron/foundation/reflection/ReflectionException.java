package com.xjtu.iron.foundation.reflection;

/**
 * 表示基础反射操作失败。
 */
public class ReflectionException extends RuntimeException {

    public ReflectionException(String message) {
        super(message);
    }

    public ReflectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
