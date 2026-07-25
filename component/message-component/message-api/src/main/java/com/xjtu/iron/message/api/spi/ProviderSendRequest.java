package com.xjtu.iron.message.api.spi;

import com.xjtu.iron.message.api.MessageDestination;

import java.util.Map;

/**
 * 表示 core 交给具体 Provider 的已序列化发送请求。
 *
 * @param destination 逻辑消息目的地
 * @param messageId 统一消息标识
 * @param key 消息键
 * @param headers 完整消息头
 * @param payload 已序列化消息体
 * @param providerProperties Provider 专属扩展属性
 */
public record ProviderSendRequest(
        MessageDestination destination,
        String messageId,
        String key,
        Map<String, String> headers,
        byte[] payload,
        Map<String, String> providerProperties) {

    /**
     * 对可变字段执行防御性复制。
     */
    public ProviderSendRequest {
        // 消息头标准化为空只读映射或只读副本。
        headers = headers == null ? Map.of() : Map.copyOf(headers);
        // 消息字节必须复制，防止发送过程中被业务线程修改。
        payload = payload == null ? null : payload.clone();
        // Provider 属性同样不可由外部继续修改。
        providerProperties = providerProperties == null
                ? Map.of()
                : Map.copyOf(providerProperties);
    }

    /**
     * 返回消息体副本。
     *
     * @return 消息体副本
     */
    @Override
    public byte[] payload() {
        // record 默认访问器会直接暴露数组，因此必须覆盖并复制。
        return payload == null ? null : payload.clone();
    }
}
