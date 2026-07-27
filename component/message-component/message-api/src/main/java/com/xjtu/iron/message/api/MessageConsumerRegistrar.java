package com.xjtu.iron.message.api;

/**
 * 面向业务代码的统一消费者注册接口。
 */
public interface MessageConsumerRegistrar {

    /**
     * 注册并启动一个普通消息消费者。
     *
     * @param definition 消费者定义
     * @param handler 业务处理器
     * @param <T> 业务消息体类型
     * @return 可关闭订阅句柄
     */
    <T> MessageSubscription subscribe(ConsumerDefinition<T> definition, MessageHandler<T> handler);
}
