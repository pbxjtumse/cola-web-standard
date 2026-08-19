package com.xjtu.iron.message.spi;

import com.xjtu.iron.message.api.consume.ConsumeDecision;

/**
 * 定义 Provider 调用 core 的入站消息监听器。
 */
@FunctionalInterface
public interface ProviderMessageListener {

    /**
     * 处理一条 Provider 原始消息。
     *
     * @param message 原始入站消息
     * @return 公共消费决策
     */
    ConsumeDecision onMessage(ProviderInboundMessage message);
}
