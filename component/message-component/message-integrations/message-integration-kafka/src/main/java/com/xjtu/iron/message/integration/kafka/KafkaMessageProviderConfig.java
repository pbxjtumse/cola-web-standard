package com.xjtu.iron.message.integration.kafka;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 表示 Kafka 基础 Provider 配置。
 *
 * @param bootstrapServers Kafka Bootstrap Server 列表
 * @param clientId 客户端基础标识
 * @param pollTimeout Consumer 单次 poll 超时
 * @param retryBackoff 一期 RETRY 决策后的本地固定退避
 * @param producerProperties 额外 Producer 原生配置
 * @param consumerProperties 额外 Consumer 原生配置
 */
public record KafkaMessageProviderConfig(
        String bootstrapServers,
        String clientId,
        Duration pollTimeout,
        Duration retryBackoff,
        Map<String, Object> producerProperties,
        Map<String, Object> consumerProperties) {

    /**
     * 校验并复制 Kafka 配置。
     */
    public KafkaMessageProviderConfig {
        // Bootstrap Server 必须存在。
        if (bootstrapServers == null || bootstrapServers.isBlank()) {
            // 没有地址无法发现 Broker。
            throw new IllegalArgumentException("bootstrapServers must not be blank");
        }
        // 去除地址首尾空白。
        bootstrapServers = bootstrapServers.trim();
        // clientId 必须存在。
        if (clientId == null || clientId.isBlank()) {
            // clientId 用于 Broker 日志和指标区分客户端。
            throw new IllegalArgumentException("clientId must not be blank");
        }
        // 去除 clientId 首尾空白。
        clientId = clientId.trim();
        // pollTimeout 必须为正数。
        if (pollTimeout == null || pollTimeout.isZero() || pollTimeout.isNegative()) {
            // 非正 poll 超时不合法。
            throw new IllegalArgumentException("pollTimeout must be positive");
        }
        // retryBackoff 允许为零，但不能为负数。
        if (retryBackoff == null || retryBackoff.isNegative()) {
            // null 或负值都属于非法配置。
            throw new IllegalArgumentException("retryBackoff must not be negative");
        }
        // 复制 Producer 配置。
        producerProperties = producerProperties == null || producerProperties.isEmpty()
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(producerProperties));
        // 复制 Consumer 配置。
        consumerProperties = consumerProperties == null || consumerProperties.isEmpty()
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(consumerProperties));
    }

    /**
     * 创建常用默认配置。
     *
     * @param bootstrapServers Bootstrap Server
     * @param clientId 客户端标识
     * @return 默认配置
     */
    public static KafkaMessageProviderConfig defaults(
            String bootstrapServers,
            String clientId) {
        // 使用一秒 poll 和一秒失败退避。
        return new KafkaMessageProviderConfig(
                bootstrapServers,
                clientId,
                Duration.ofSeconds(1),
                Duration.ofSeconds(1),
                Map.of(),
                Map.of());
    }
}
