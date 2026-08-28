package com.xjtu.iron.message.spi;

/**
 * Provider 入站消息监听器。
 */
@FunctionalInterface
public interface ProviderMessageListener {

    /**
     * 处理一条 Provider 原始消息，并返回 Provider 可映射的消费结果。
     *
     * @param message 原始入站消息
     * @return 消费结果
     */
    ProviderConsumeResult onMessage(ProviderInboundMessage message);
}
