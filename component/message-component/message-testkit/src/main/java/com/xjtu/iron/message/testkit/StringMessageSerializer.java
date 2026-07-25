package com.xjtu.iron.message.testkit;

import com.xjtu.iron.message.api.MessageSerializer;

import java.nio.charset.StandardCharsets;

/**
 * 仅用于示例和测试的 UTF-8 字符串序列化器。
 */
public final class StringMessageSerializer implements MessageSerializer {

    /**
     * 将 String 转换为 UTF-8 字节。
     */
    @Override
    public byte[] serialize(Object payload) {
        // 第一版测试序列化器只接受 String。
        if (!(payload instanceof String text)) {
            // 明确拒绝隐式 toString，避免测试掩盖真实序列化问题。
            throw new IllegalArgumentException("StringMessageSerializer only supports String payload");
        }
        // 使用固定 UTF-8 编码，避免平台默认编码差异。
        return text.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 将 UTF-8 字节转换为 String。
     */
    @Override
    public <T> T deserialize(byte[] payload, Class<T> targetType) {
        // 仅支持 String 目标类型。
        if (targetType != String.class) {
            // 明确拒绝不支持类型。
            throw new IllegalArgumentException("StringMessageSerializer only supports String target type");
        }
        // 原始字节不能为空。
        if (payload == null) {
            // null 字节没有稳定反序列化语义。
            throw new IllegalArgumentException("payload must not be null");
        }
        // 创建 UTF-8 字符串。
        String value = new String(payload, StandardCharsets.UTF_8);
        // 由目标类型执行安全转换。
        return targetType.cast(value);
    }
}
