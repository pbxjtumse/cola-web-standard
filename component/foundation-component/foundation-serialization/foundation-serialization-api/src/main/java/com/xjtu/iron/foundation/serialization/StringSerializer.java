package com.xjtu.iron.foundation.serialization;

/**
 * 定义对象和文本之间的序列化协议。
 */
public interface StringSerializer extends Serializer {

    String serializeToString(Object value, SerializationContext context, SerializationOptions options);

    <T> T deserializeFromString(String content,
                                TypeDescriptor<T> targetType,
                                SerializationContext context,
                                SerializationOptions options);

    default String serializeToString(Object value) {
        return serializeToString(value, SerializationContext.empty(), SerializationOptions.defaults());
    }
}
