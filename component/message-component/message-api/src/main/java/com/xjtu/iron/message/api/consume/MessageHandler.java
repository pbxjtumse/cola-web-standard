package com.xjtu.iron.message.api.consume;

import com.xjtu.iron.message.api.model.MessageEnvelope;

/**
 * 定义普通消息的业务处理函数。
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
     * @return SUCCESS 或 RETRY
     */
    ConsumeDecision handle(MessageEnvelope<T> message, ConsumeContext context);
}
