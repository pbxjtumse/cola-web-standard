package com.xjtu.iron.foundation.serialization.jackson;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * 创建与 Web MVC 配置隔离的 Jackson ObjectMapper。
 */
public final class JacksonObjectMapperFactory {

    private JacksonObjectMapperFactory() {
    }

    public static ObjectMapper createDefault() {
        return create(JacksonConfiguration.defaults(), java.util.List.of(), java.util.List.of());
    }

    public static ObjectMapper create(JacksonConfiguration configuration,
                                      Iterable<? extends JacksonModuleProvider> moduleProviders,
                                      Iterable<? extends JacksonSerializerCustomizer> customizers) {
        JacksonConfiguration actual = configuration == null ? JacksonConfiguration.defaults() : configuration;
        JsonMapper.Builder builder = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                        actual.isFailOnUnknownProperties())
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS,
                        actual.isWriteDatesAsTimestamps());

        if (moduleProviders != null) {
            for (JacksonModuleProvider provider : moduleProviders) {
                if (provider != null) {
                    Module module = provider.module();
                    if (module != null) {
                        builder.addModule(module);
                    }
                }
            }
        }

        ObjectMapper mapper = builder.build();
        mapper.setSerializationInclusion(
                actual.isIncludeNullValues() ? JsonInclude.Include.ALWAYS : JsonInclude.Include.NON_NULL
        );
        if (customizers != null) {
            for (JacksonSerializerCustomizer customizer : customizers) {
                if (customizer != null) {
                    customizer.customize(mapper);
                }
            }
        }
        return mapper;
    }
}
