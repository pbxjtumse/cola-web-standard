package com.xjtu.iron.message.api;

import java.util.Objects;

/**
 * 定义一个普通消息消费者。
 *
 * @param destination 逻辑目的地
 * @param consumerGroup 消费组或订阅名称
 * @param payloadType 业务消息体类型
 * @param <T> 业务消息体类型
 */
public record ConsumerDefinition<T>(
        MessageDestination destination,
        String consumerGroup,
        Class<T> payloadType) {

    /**
     * 校验消费者定义。
     */
    public ConsumerDefinition {
        // 逻辑目的地不能为空。
        destination = Objects.requireNonNull(destination, "destination must not be null");
        // 消费组不能为空或空白。
        if (consumerGroup == null || consumerGroup.isBlank()) {
            // 不同 Provider 都依赖消费组或订阅名区分消费进度。
            throw new IllegalArgumentException("consumerGroup must not be blank");
        }
        // 去除消费组首尾空白。
        consumerGroup = consumerGroup.trim();
        // 反序列化目标类型不能为空。
        payloadType = Objects.requireNonNull(payloadType, "payloadType must not be null");
    }

    /**
     * 创建消费者定义。
     *
     * @param destination 逻辑目的地
     * @param consumerGroup 消费组
     * @param payloadType 消息体类型
     * @param <T> 消息体类型
     * @return 消费者定义
     */
    public static <T> ConsumerDefinition<T> of(
            MessageDestination destination,
            String consumerGroup,
            Class<T> payloadType) {
        // 使用静态工厂提升调用处可读性。
        return new ConsumerDefinition<>(destination, consumerGroup, payloadType);
    }
}
