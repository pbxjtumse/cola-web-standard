package com.xjtu.iron.message.api;

/**
 * 定义业务消费者注册入口。
 */
public interface MessageConsumerRegistrar {

    /**
     * 注册并启动一个消费者。
     *
     * @param definition 消费者定义
     * @param <T> 业务消息体类型
     * @return 可关闭的消费者句柄
     */
    <T> MessageConsumer subscribe(ConsumerDefinition<T> definition);
}
