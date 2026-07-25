package com.xjtu.iron.message.api.spi;

import com.xjtu.iron.message.api.ConsumeDecision;

/**
 * 定义 Provider 调用 core 的入站消息监听器。
 */
@FunctionalInterface
public interface ProviderMessageListener {

    /**
     * 将一条原始消息交给 core 处理。
     *
     * @param message Provider 原始消息
     * @return Provider 需要翻译为 ACK 或重投的消费决策
     */
    ConsumeDecision onMessage(ProviderInboundMessage message);
}
