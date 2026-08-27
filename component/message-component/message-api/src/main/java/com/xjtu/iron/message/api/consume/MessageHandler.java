package com.xjtu.iron.message.api.consume;

import com.xjtu.iron.message.api.model.MessageEnvelope;

/**
 * 业务消费处理器接口，表示一条统一消息进入业务代码后的处理入口。
 *
 * <p>Provider 收到 Kafka、Pulsar 或 RocketMQ 的原生消息后，会先由 core 解码成 {@code MessageEnvelope}，
 * 再通过该接口交给业务处理。业务处理完成后返回 {@code ConsumeDecision}，由 Provider 映射成 ACK、重试或稍后再消费。</p>
 */
@FunctionalInterface
public interface MessageHandler<T> {

    /**
     * 处理消息并返回明确消费决策。
     *
     * @param message 已还原的统一消息信封
     * @param context 当前投递运行时上下文
     * @return SUCCESS 或 RETRY
     */
    ConsumeDecision handle(MessageEnvelope<T> message, ConsumeContext context);
}
