package com.xjtu.iron.message.testkit;

import com.xjtu.iron.message.api.MessageSerializer;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * 仅用于测试和最小 Demo 的 UTF-8 String 序列化器。
 */
public final class Utf8StringMessageSerializer implements MessageSerializer {

    /** 文本媒体类型。 */
    public static final String CONTENT_TYPE = "text/plain;charset=UTF-8";

    /**
     * 返回文本媒体类型。
     */
    @Override
    public String contentType() {
        // 返回稳定媒体类型。
        return CONTENT_TYPE;
    }

    /**
     * 将 String 序列化为 UTF-8 字节。
     */
    @Override
    public byte[] serialize(Object payload) {
        // 消息体不能为空。
        Objects.requireNonNull(payload, "payload must not be null");
        // 只允许 String，避免测试序列化器被误用于生产对象。
        if (!(payload instanceof String text)) {
            // 明确提示应使用 Jackson 等生产序列化器。
            throw new IllegalArgumentException(
                    "Utf8StringMessageSerializer only supports String payload");
        }
        // 使用 UTF-8 编码。
        return text.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 将 UTF-8 字节反序列化为 String。
     */
    @Override
    public <T> T deserialize(byte[] payload, Class<T> targetType) {
        // 字节数组不能为空。
        Objects.requireNonNull(payload, "payload must not be null");
        // 只允许 String 目标类型。
        if (targetType != String.class) {
            // 明确提示测试序列化器限制。
            throw new IllegalArgumentException(
                    "Utf8StringMessageSerializer only supports String targetType");
        }
        // 创建 UTF-8 String。
        String value = new String(payload, StandardCharsets.UTF_8);
        // targetType 已验证为 String，因此转换安全。
        return targetType.cast(value);
    }
}
