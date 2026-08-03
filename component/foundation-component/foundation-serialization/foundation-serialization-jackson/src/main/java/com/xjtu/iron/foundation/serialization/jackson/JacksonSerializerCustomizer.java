package com.xjtu.iron.foundation.serialization.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 在 ObjectMapper 冻结给序列化器之前执行受控定制。
 */
@FunctionalInterface
public interface JacksonSerializerCustomizer {

    void customize(ObjectMapper objectMapper);
}
