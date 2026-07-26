package com.xjtu.iron.message.spi;

import java.util.Objects;

/**
 * 表示 core 交给 Provider 的普通消费订阅请求。
 *
 * @param destination 已解析物理目的地
 * @param consumerGroup 消费组或订阅名称
 * @param listener core 入站监听器
 */
public record ProviderSubscriptionRequest(
        ProviderDestination destination,
        String consumerGroup,
        ProviderMessageListener listener) {

    /**
     * 校验订阅请求。
     */
    public ProviderSubscriptionRequest {
        // 物理目的地不能为空。
        destination = Objects.requireNonNull(destination, "destination must not be null");
        // 消费组不能为空。
        if (consumerGroup == null || consumerGroup.isBlank()) {
            // 所有一期 Provider 都需要消费组或订阅名。
            throw new IllegalArgumentException("consumerGroup must not be blank");
        }
        // 去除消费组首尾空白。
        consumerGroup = consumerGroup.trim();
        // 入站监听器不能为空。
        listener = Objects.requireNonNull(listener, "listener must not be null");
    }
}
