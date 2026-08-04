package com.xjtu.iron.foundation.serialization;

/**
 * 序列化器统一接口。
 */
public interface Serializer {

    SerializedPayload serialize(Object value);

    SerializedPayload serialize(Object value, SerializationOptions options, SerializationContext context);

    <T> T deserialize(SerializedPayload payload, Class<T> targetType);

    <T> T deserialize(SerializedPayload payload, TypeReference<T> targetType);
}
