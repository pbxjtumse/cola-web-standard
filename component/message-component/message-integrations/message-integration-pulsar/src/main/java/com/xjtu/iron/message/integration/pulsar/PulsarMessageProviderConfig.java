package com.xjtu.iron.message.integration.pulsar;

import java.time.Duration;

/**
 * 表示 Pulsar 稳定 Java Client 的一期基础配置。
 *
 * @param serviceUrl Pulsar 服务地址
 * @param operationTimeout 客户端操作超时
 * @param negativeAckRedeliveryDelay Negative ACK 后重新投递延迟
 * @param receiverQueueSize Consumer 接收队列大小
 * @param authenticationToken 可选 Token 认证值
 */
public record PulsarMessageProviderConfig(
        String serviceUrl,
        Duration operationTimeout,
        Duration negativeAckRedeliveryDelay,
        int receiverQueueSize,
        String authenticationToken) {

    /**
     * 校验并标准化 Pulsar 配置。
     */
    public PulsarMessageProviderConfig {
        // serviceUrl 必须存在。
        if (serviceUrl == null || serviceUrl.isBlank()) {
            // 没有服务地址无法创建客户端。
            throw new IllegalArgumentException("serviceUrl must not be blank");
        }
        // 去除地址首尾空白。
        serviceUrl = serviceUrl.trim();
        // operationTimeout 必须为正数。
        if (operationTimeout == null
                || operationTimeout.isZero()
                || operationTimeout.isNegative()) {
            // 非正超时无意义。
            throw new IllegalArgumentException("operationTimeout must be positive");
        }
        // Negative ACK 延迟允许为零但不能为负数。
        if (negativeAckRedeliveryDelay == null
                || negativeAckRedeliveryDelay.isNegative()) {
            // null 或负数都属于非法配置。
            throw new IllegalArgumentException(
                    "negativeAckRedeliveryDelay must not be negative");
        }
        // 接收队列不能为负数，零表示禁用预取。
        if (receiverQueueSize < 0) {
            // 拒绝非法队列大小。
            throw new IllegalArgumentException("receiverQueueSize must not be negative");
        }
        // 空白 Token 统一转换为 null。
        authenticationToken = authenticationToken == null
                || authenticationToken.isBlank()
                ? null
                : authenticationToken.trim();
    }

    /**
     * 创建无鉴权默认配置。
     *
     * @param serviceUrl Pulsar 服务地址
     * @return 默认配置
     */
    public static PulsarMessageProviderConfig defaults(String serviceUrl) {
        // 默认三秒操作超时、一秒负确认重投延迟和 1000 条接收队列。
        return new PulsarMessageProviderConfig(
                serviceUrl,
                Duration.ofSeconds(3),
                Duration.ofSeconds(1),
                1000,
                null);
    }
}
