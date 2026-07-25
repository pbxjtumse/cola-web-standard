package com.xjtu.iron.message.api;

/**
 * 描述一个业务消费者的最小定义。
 *
 * @param destination 消息目的地
 * @param consumerGroup 消费组名称
 * @param payloadType 反序列化目标类型
 * @param handler 业务处理函数
 * @param <T> 业务消息体类型
 */
public record ConsumerDefinition<T>(
        MessageDestination destination,
        String consumerGroup,
        Class<T> payloadType,
        MessageHandler<T> handler) {
}
