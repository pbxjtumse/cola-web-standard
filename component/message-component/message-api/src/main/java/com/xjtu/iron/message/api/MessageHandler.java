package com.xjtu.iron.message.api;

/**
 * 定义业务消息处理函数。
 *
 * @param <T> 反序列化后的业务消息体类型
 */
@FunctionalInterface
public interface MessageHandler<T> {

    /**
     * 处理一条业务消息并返回消费决策。
     *
     * @param payload 业务消息体
     * @param context 消费上下文
     * @return SUCCESS 或 RETRY；不允许返回 null
     */
    ConsumeDecision handle(T payload, ConsumeContext context);
}
