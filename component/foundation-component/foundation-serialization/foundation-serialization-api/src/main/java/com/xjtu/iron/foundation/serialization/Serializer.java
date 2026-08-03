package com.xjtu.iron.foundation.serialization;

/**
 * 定义对象和二进制载荷之间的序列化协议。
 */
public interface Serializer {

    /** 返回当前序列化器的数据格式。 */
    SerializationFormat format();

    /**
     * 序列化对象。
     */
    byte[] serialize(Object value, SerializationContext context, SerializationOptions options);

    /**
     * 根据目标类型反序列化对象。
     */
    <T> T deserialize(byte[] content,
                      TypeDescriptor<T> targetType,
                      SerializationContext context,
                      SerializationOptions options);

    default byte[] serialize(Object value) {
        return serialize(value, SerializationContext.empty(), SerializationOptions.defaults());
    }

    default <T> T deserialize(byte[] content, Class<T> targetType) {
        return deserialize(
                content,
                TypeDescriptor.of(targetType),
                SerializationContext.empty(),
                SerializationOptions.defaults()
        );
    }

    default <T> T deserialize(byte[] content, TypeDescriptor<T> targetType) {
        return deserialize(content, targetType, SerializationContext.empty(), SerializationOptions.defaults());
    }

    /**
     * 直接生成携带类型和 schema 信息的载荷对象。
     */
    default SerializedPayload serializePayload(Object value,
                                               String schemaVersion,
                                               SerializationOptions options) {
        SerializationOptions actual = options == null ? SerializationOptions.defaults() : options;
        byte[] content = serialize(
                value,
                new SerializationContext(null, schemaVersion, java.util.Map.of()),
                actual
        );
        return new SerializedPayload(
                content,
                format(),
                value == null ? null : value.getClass().getName(),
                schemaVersion,
                actual.getCharset()
        );
    }
}
