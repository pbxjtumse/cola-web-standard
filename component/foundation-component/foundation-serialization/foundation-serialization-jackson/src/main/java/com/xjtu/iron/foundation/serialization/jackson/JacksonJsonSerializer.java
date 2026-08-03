package com.xjtu.iron.foundation.serialization.jackson;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.xjtu.iron.foundation.serialization.SerializationContext;
import com.xjtu.iron.foundation.serialization.SerializationException;
import com.xjtu.iron.foundation.serialization.SerializationFormat;
import com.xjtu.iron.foundation.serialization.SerializationOperation;
import com.xjtu.iron.foundation.serialization.SerializationOptions;
import com.xjtu.iron.foundation.serialization.StringSerializer;
import com.xjtu.iron.foundation.serialization.TypeDescriptor;

import java.nio.charset.Charset;
import java.util.Objects;

/**
 * 基于 Jackson 2.x 的 JSON 序列化实现。
 *
 * <p>构造时复制 ObjectMapper，避免外部后续修改导致消息线协议静默变化。</p>
 */
public final class JacksonJsonSerializer implements StringSerializer {

    /** 复制并隔离后的 Jackson ObjectMapper。 */
    private final ObjectMapper objectMapper;

    public JacksonJsonSerializer(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null").copy();
    }

    @Override
    public SerializationFormat format() {
        return SerializationFormat.JSON;
    }

    @Override
    public byte[] serialize(Object value, SerializationContext context, SerializationOptions options) {
        SerializationOptions actual = options == null ? SerializationOptions.defaults() : options;
        try {
            ObjectWriter writer = JacksonSerializationOptionsMapper.writer(objectMapper, actual);
            String json = writer.writeValueAsString(value);
            byte[] content = json.getBytes(actual.getCharset());
            enforceLimit(content.length, actual, SerializationOperation.SERIALIZE);
            return content;
        } catch (JsonProcessingException exception) {
            throw failure(SerializationOperation.SERIALIZE, value == null ? null : value.getClass().getName(), 0, exception);
        }
    }

    @Override
    public String serializeToString(Object value, SerializationContext context, SerializationOptions options) {
        SerializationOptions actual = options == null ? SerializationOptions.defaults() : options;
        try {
            ObjectWriter writer = JacksonSerializationOptionsMapper.writer(objectMapper, actual);
            String content = writer.writeValueAsString(value);
            enforceLimit(content.getBytes(actual.getCharset()).length, actual, SerializationOperation.SERIALIZE);
            return content;
        } catch (JsonProcessingException exception) {
            throw failure(SerializationOperation.SERIALIZE, value == null ? null : value.getClass().getName(), 0, exception);
        }
    }

    @Override
    public <T> T deserialize(byte[] content,
                             TypeDescriptor<T> targetType,
                             SerializationContext context,
                             SerializationOptions options) {
        SerializationOptions actual = options == null ? SerializationOptions.defaults() : options;
        if (content == null) {
            return null;
        }
        enforceLimit(content.length, actual, SerializationOperation.DESERIALIZE);
        try {
            JavaType javaType = JacksonTypeFactorySupport.toJavaType(objectMapper.getTypeFactory(), targetType);
            ObjectReader reader = JacksonSerializationOptionsMapper.reader(objectMapper, javaType, actual);
            return reader.readValue(new String(content, actual.getCharset()));
        } catch (Exception exception) {
            throw failure(SerializationOperation.DESERIALIZE, targetType.toString(), content.length, exception);
        }
    }

    @Override
    public <T> T deserializeFromString(String content,
                                       TypeDescriptor<T> targetType,
                                       SerializationContext context,
                                       SerializationOptions options) {
        SerializationOptions actual = options == null ? SerializationOptions.defaults() : options;
        if (content == null) {
            return null;
        }
        Charset charset = actual.getCharset();
        byte[] bytes = content.getBytes(charset);
        enforceLimit(bytes.length, actual, SerializationOperation.DESERIALIZE);
        try {
            JavaType javaType = JacksonTypeFactorySupport.toJavaType(objectMapper.getTypeFactory(), targetType);
            ObjectReader reader = JacksonSerializationOptionsMapper.reader(objectMapper, javaType, actual);
            return reader.readValue(content);
        } catch (Exception exception) {
            throw failure(SerializationOperation.DESERIALIZE, targetType.toString(), bytes.length, exception);
        }
    }

    private static void enforceLimit(int contentLength,
                                     SerializationOptions options,
                                     SerializationOperation operation) {
        if (contentLength > options.getMaxBytes()) {
            throw new SerializationException(
                    "serialization content exceeds limit: " + options.getMaxBytes(),
                    operation,
                    null,
                    contentLength,
                    null
            );
        }
    }

    private static SerializationException failure(SerializationOperation operation,
                                                  String targetType,
                                                  int contentLength,
                                                  Throwable cause) {
        return new SerializationException(
                "Jackson failed to " + operation.name().toLowerCase() + " type " + targetType,
                operation,
                targetType,
                contentLength,
                cause
        );
    }
}
