package com.xjtu.iron.foundation.serialization.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 创建 Jackson JSON 序列化器。
 */
public final class JacksonSerializerFactory {

    private JacksonSerializerFactory() {
    }

    public static JacksonJsonSerializer createDefault() {
        return new JacksonJsonSerializer(JacksonObjectMapperFactory.createDefault());
    }

    public static JacksonJsonSerializer create(ObjectMapper objectMapper) {
        return new JacksonJsonSerializer(objectMapper);
    }
}
