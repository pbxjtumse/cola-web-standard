package com.xjtu.iron.foundation.serialization.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Jackson ObjectMapper 定制扩展点。
 */
@FunctionalInterface
public interface JacksonSerializerCustomizer {

    void customize(ObjectMapper objectMapper);
}
