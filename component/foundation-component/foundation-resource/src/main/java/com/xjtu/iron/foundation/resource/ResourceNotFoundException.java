package com.xjtu.iron.foundation.resource;

/**
 * 资源不存在异常。
 */
public final class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String location) {
        super("resource not found: " + location);
    }
}
