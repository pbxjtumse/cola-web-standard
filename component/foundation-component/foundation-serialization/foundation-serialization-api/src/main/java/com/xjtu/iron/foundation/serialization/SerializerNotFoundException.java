package com.xjtu.iron.foundation.serialization;

/**
 * 表示注册表中不存在请求的序列化器。
 */
public class SerializerNotFoundException extends RuntimeException {

    public SerializerNotFoundException(String message) {
        super(message);
    }
}
