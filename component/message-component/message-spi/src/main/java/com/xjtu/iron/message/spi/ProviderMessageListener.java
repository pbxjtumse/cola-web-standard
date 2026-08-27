package com.xjtu.iron.message.spi;

import com.xjtu.iron.message.api.consume.ConsumeDecision;

/**
 * Provider 入站消息监听器。
 *
 * <p>具体 Provider 从 Broker 拉取或接收原生消息后，会先封装为 {@code ProviderInboundMessage}，
 * 再调用该监听器交给 message-core 解码和业务处理。</p>
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
