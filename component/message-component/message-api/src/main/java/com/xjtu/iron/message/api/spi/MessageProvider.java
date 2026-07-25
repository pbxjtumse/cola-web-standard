package com.xjtu.iron.message.api.spi;

import com.xjtu.iron.message.api.MessageConsumer;

import java.util.Set;
import java.util.concurrent.CompletionStage;

/**
 * 定义 Kafka、RocketMQ、Pulsar 等中间件适配器的最小 SPI。
 */
public interface MessageProvider extends AutoCloseable {

    /**
     * 返回稳定且唯一的 Provider 名称。
     *
     * @return Provider 名称
     */
    String name();

    /**
     * 返回当前 Provider 支持的公共能力集合。
     *
     * @return 不可变能力集合
     */
    Set<MessageCapability> capabilities();

    /**
     * 异步发送一条已序列化消息。
     *
     * @param request Provider 发送请求
     * @return Provider 标准结果
     */
    CompletionStage<ProviderSendResult> send(ProviderSendRequest request);

    /**
     * 创建并启动一个基础消费者。
     *
     * @param subscription 基础订阅请求
     * @return 可关闭消费者句柄
     */
    MessageConsumer subscribe(ProviderSubscription subscription);

    /**
     * 关闭 Provider 持有的客户端资源。
     */
    @Override
    void close();
}
