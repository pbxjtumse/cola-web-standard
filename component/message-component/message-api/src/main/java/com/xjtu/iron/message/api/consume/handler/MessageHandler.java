package com.xjtu.iron.message.api.consume.handler;

import com.xjtu.iron.message.api.consume.context.ConsumeContext;
import com.xjtu.iron.message.api.consume.decision.ConsumeDecision;
import com.xjtu.iron.message.api.model.MessageEnvelope;

/**
 * 业务消费处理器接口，表示一条统一消息进入业务代码后的处理入口。
 *
 * @param <T> 业务消息体类型
 */
@FunctionalInterface
public interface MessageHandler<T> {

    /**
     * 处理消息并返回明确消费决策。
     *
     * @param message 已还原的统一消息信封
     * @param context 当前投递运行时上下文
     * @return ACK、RETRY、DISCARD 或 DEAD_LETTER；返回 null 时 core 会按 RETRY 处理
     */
    ConsumeDecision handle(MessageEnvelope<T> message, ConsumeContext context);
}
