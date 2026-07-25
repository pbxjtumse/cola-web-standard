package com.xjtu.iron.message.api;

import java.time.Duration;
import java.util.Map;

/**
 * 表示一次消息发送的调用选项。
 *
 * @param confirmationTimeout 同步调用等待 Provider 确认的最长时间
 * @param providerProperties 显式传递给具体 Provider 的扩展属性
 */
public record SendOptions(
        Duration confirmationTimeout,
        Map<String, String> providerProperties) {

    /** 第一版默认同步确认超时时间。 */
    private static final Duration DEFAULT_CONFIRMATION_TIMEOUT = Duration.ofSeconds(5);

    /**
     * 执行默认值处理和防御性复制。
     */
    public SendOptions {
        // 未指定超时时间时使用统一默认值。
        confirmationTimeout = confirmationTimeout == null
                ? DEFAULT_CONFIRMATION_TIMEOUT
                : confirmationTimeout;
        // 未指定 Provider 属性时使用空只读映射。
        providerProperties = providerProperties == null
                ? Map.of()
                : Map.copyOf(providerProperties);
    }

    /**
     * 返回默认发送选项。
     *
     * @return 默认发送选项
     */
    public static SendOptions defaults() {
        // 统一创建默认配置，避免业务散落魔法值。
        return new SendOptions(DEFAULT_CONFIRMATION_TIMEOUT, Map.of());
    }
}
