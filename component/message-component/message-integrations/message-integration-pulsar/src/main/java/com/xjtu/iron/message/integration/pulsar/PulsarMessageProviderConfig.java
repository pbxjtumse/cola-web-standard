package com.xjtu.iron.message.integration.pulsar;

import java.time.Duration;

/**
 * 表示 Pulsar Provider 第一版所需的基础配置。
 *
 * @param serviceUrl Pulsar 服务地址
 * @param operationTimeout 客户端操作超时时间
 * @param receiverQueueSize Consumer 接收队列大小
 */
public record PulsarMessageProviderConfig(
        String serviceUrl,
        Duration operationTimeout,
        int receiverQueueSize) {

    /**
     * 执行默认值和参数校验。
     */
    public PulsarMessageProviderConfig {
        // 服务地址不能为空。
        if (serviceUrl == null || serviceUrl.isBlank()) {
            // 无服务地址时客户端无法启动。
            throw new IllegalArgumentException("serviceUrl must not be blank");
        }
        // 未指定操作超时时使用三十秒。
        operationTimeout = operationTimeout == null
                ? Duration.ofSeconds(30)
                : operationTimeout;
        // 操作超时必须为正数。
        if (operationTimeout.isZero() || operationTimeout.isNegative()) {
            // 非正值没有有效超时语义。
            throw new IllegalArgumentException("operationTimeout must be positive");
        }
        // 接收队列至少容纳一条消息。
        if (receiverQueueSize <= 0) {
            // 非正队列大小属于无效配置。
            throw new IllegalArgumentException("receiverQueueSize must be positive");
        }
    }

    /**
     * 创建默认配置。
     *
     * @param serviceUrl Pulsar 服务地址
     * @return 默认配置
     */
    public static PulsarMessageProviderConfig defaults(String serviceUrl) {
        // 使用三十秒操作超时和一千条接收队列。
        return new PulsarMessageProviderConfig(
                serviceUrl,
                Duration.ofSeconds(30),
                1000);
    }
}
