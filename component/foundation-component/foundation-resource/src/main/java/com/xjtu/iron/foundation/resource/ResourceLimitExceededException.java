package com.xjtu.iron.foundation.resource;

/**
 * 表示读取资源时超过配置的最大字节数。
 */
public class ResourceLimitExceededException extends RuntimeException {

    public ResourceLimitExceededException(String message) {
        super(message);
    }
}
