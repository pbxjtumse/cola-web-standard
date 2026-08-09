package com.xjtu.iron.message.codec.jackson;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.xjtu.iron.message.api.MessageSerializer;

import java.io.IOException;
import java.util.Objects;

/**
 * 基于 Jackson 的 JSON 消息体序列化器。
 *
 * <p>该类只负责 MessageEnvelope.payload 的 JSON 序列化和反序列化，
 * 不处理 x-iron-message-* 系统消息头，也不处理 Provider 物理目的地。
 * 这些消息线级协议由 message-core 的 MessageWireCodec 统一处理。</p>
 */
public final class JacksonMessageSerializer implements MessageSerializer {

    /** JSON 媒体类型。 */
    public static final String CONTENT_TYPE = "application/json";

    /** Jackson ObjectMapper。 */
    private final ObjectMapper objectMapper;

    /**
     * 使用适合普通 Java 和 Java Time 类型的默认 ObjectMapper。
     */
    public JacksonMessageSerializer() {
        // JsonMapper Builder 比直接 new ObjectMapper 更容易显式组合模块。
        this(JsonMapper.builder()
                // 注册 Java Time 模块以支持 Instant 等类型。
                .addModule(new JavaTimeModule())
                // 发现 classpath 中其他 Jackson 模块。
                .findAndAddModules()
                // 构建 ObjectMapper。
                .build());
    }

    /**
     * 使用业务自定义 ObjectMapper。
     *
     * @param objectMapper ObjectMapper
     */
    public JacksonMessageSerializer(ObjectMapper objectMapper) {
        // ObjectMapper 不能为空。
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    /**
     * 返回 JSON 媒体类型。
     */
    @Override
    public String contentType() {
        // 返回稳定媒体类型。
        return CONTENT_TYPE;
    }

    /**
     * 将业务对象序列化为 JSON UTF-8 字节。
     */
    @Override
    public byte[] serialize(Object payload) {
        // 消息体不能为空。
        Objects.requireNonNull(payload, "payload must not be null");
        // 捕获 Jackson 受检异常并转换为运行时异常，交给 core 标准分类。
        try {
            // 直接输出 UTF-8 JSON 字节，避免中间 String 分配。
            return objectMapper.writeValueAsBytes(payload);
        } catch (JsonProcessingException exception) {
            // 附带业务类型便于诊断。
            throw new IllegalArgumentException(
                    "failed to serialize message payload: " + payload.getClass().getName(),
                    exception);
        }
    }

    /**
     * 将 JSON 字节反序列化为指定业务类型。
     */
    @Override
    public <T> T deserialize(byte[] payload, Class<T> targetType) {
        // 原始字节不能为空。
        Objects.requireNonNull(payload, "payload must not be null");
        // 目标类型不能为空。
        Objects.requireNonNull(targetType, "targetType must not be null");
        // 捕获 Jackson 受检异常并转换为运行时异常。
        try {
            // 根据消费者定义的目标类型反序列化。
            return objectMapper.readValue(payload, targetType);
        } catch (IOException exception) {
            // 附带目标类型便于定位消息结构不兼容问题。
            throw new IllegalArgumentException(
                    "failed to deserialize message payload to " + targetType.getName(),
                    exception);
        }
    }
}
