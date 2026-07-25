package com.xjtu.iron.message.api.spi;

import com.xjtu.iron.message.api.MessageDestination;

/**
 * 表示 core 交给 Provider 的基础订阅请求。
 *
 * @param destination 逻辑消息目的地
 * @param consumerGroup 消费组名称
 * @param listener 入站消息监听器
 */
public record ProviderSubscription(
        MessageDestination destination,
        String consumerGroup,
        ProviderMessageListener listener) {
}
