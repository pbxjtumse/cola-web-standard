package com.xjtu.iron.foundation.serialization.jackson;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.xjtu.iron.foundation.serialization.SerializationOptions;

/**
 * 将单次通用选项映射为 Jackson Reader 和 Writer。
 */
public final class JacksonSerializationOptionsMapper {

    private JacksonSerializationOptionsMapper() {
    }

    public static ObjectWriter writer(ObjectMapper mapper, SerializationOptions options) {
        return options.isPrettyPrint()
                ? mapper.writerWithDefaultPrettyPrinter()
                : mapper.writer();
    }

    public static ObjectReader reader(ObjectMapper mapper,
                                      com.fasterxml.jackson.databind.JavaType targetType,
                                      SerializationOptions options) {
        ObjectReader reader = mapper.readerFor(targetType);
        return options.isFailOnUnknownProperties()
                ? reader.with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                : reader.without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }
}
