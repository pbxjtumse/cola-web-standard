package com.xjtu.iron.message.integration.kafka;

import java.time.Duration;
import java.util.Map;

/**
 * 表示 Kafka Provider 第一版所需的基础配置。
 *
 * @param bootstrapServers Kafka Broker 地址
 * @param clientId Producer 客户端标识
 * @param pollTimeout Consumer 每次 poll 的等待时间
 * @param retryBackoff 业务返回 RETRY 后的本地退避时间
 * @param producerProperties 追加或覆盖的 Kafka Producer 原生配置
 * @param consumerProperties 追加或覆盖的 Kafka Consumer 原生配置
 */
public record KafkaMessageProviderConfig(
        String bootstrapServers,
        String clientId,
        Duration pollTimeout,
        Duration retryBackoff,
        Map<String, Object> producerProperties,
        Map<String, Object> consumerProperties) {

    /**
     * 执行基础配置校验、默认值处理和防御性复制。
     */
    public KafkaMessageProviderConfig {
        // Broker 地址不能为空。
        if (bootstrapServers == null || bootstrapServers.isBlank()) {
            // 在 Provider 初始化前暴露配置问题。
            throw new IllegalArgumentException("bootstrapServers must not be blank");
        }
        // clientId 不能为空。
        if (clientId == null || clientId.isBlank()) {
            // clientId 用于 Broker 和监控识别客户端。
            throw new IllegalArgumentException("clientId must not be blank");
        }
        // 未设置 poll 超时时使用一秒。
        pollTimeout = pollTimeout == null ? Duration.ofSeconds(1) : pollTimeout;
        // poll 超时必须为正数。
        if (pollTimeout.isZero() || pollTimeout.isNegative()) {
            // 非正等待时间会造成空转或非法配置。
            throw new IllegalArgumentException("pollTimeout must be positive");
        }
        // 未设置重试退避时使用一秒。
        retryBackoff = retryBackoff == null ? Duration.ofSeconds(1) : retryBackoff;
        // 重试退避不能为负数。
        if (retryBackoff.isNegative()) {
            // 允许零退避，但不允许负数。
            throw new IllegalArgumentException("retryBackoff must not be negative");
        }
        // Producer 扩展配置转换为不可变映射。
        producerProperties = producerProperties == null
                ? Map.of()
                : Map.copyOf(producerProperties);
        // Consumer 扩展配置转换为不可变映射。
        consumerProperties = consumerProperties == null
                ? Map.of()
                : Map.copyOf(consumerProperties);
    }

    /**
     * 创建只包含必要连接信息的默认配置。
     *
     * @param bootstrapServers Kafka Broker 地址
     * @param clientId 客户端标识
     * @return 默认配置
     */
    public static KafkaMessageProviderConfig defaults(
            String bootstrapServers,
            String clientId) {
        // 使用保守默认值，原生参数仍可通过两个 properties 映射覆盖。
        return new KafkaMessageProviderConfig(
                bootstrapServers,
                clientId,
                Duration.ofSeconds(1),
                Duration.ofSeconds(1),
                Map.of(),
                Map.of());
    }
}
