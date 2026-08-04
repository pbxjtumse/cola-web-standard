package com.xjtu.iron.message.spi;

import java.util.Set;
import java.util.concurrent.CompletionStage;

/**
 * 定义 Kafka、RocketMQ、Pulsar 等基础 Provider 的最小 SPI。
 *
 * <p>该接口是一期稳定 Provider 边界，Provider 只负责普通消息发送、普通消息订阅和资源释放。
 * Producer、Consumer、ListenerContainer 等更细粒度对象不进入一期 SPI，避免把 Spring 或特定客户端模型泄露给 core。</p>
 */
public interface MessageProvider extends AutoCloseable {

    /**
     * 返回 Provider 稳定名称。
     *
     * @return 稳定名称，例如 kafka、rocketmq、pulsar
     */
    String name();

    /**
     * 返回 Provider 当前实现支持的公共能力。
     *
     * @return 不可变能力集合
     */
    Set<MessageCapability> capabilities();

    /**
     * 异步发送普通消息。
     *
     * @param request Provider 发送请求
     * @return Provider 标准结果
     */
    CompletionStage<ProviderSendResult> send(ProviderSendRequest request);

    /**
     * 创建并启动普通消息订阅。
     *
     * @param request Provider 订阅请求
     * @return Provider 订阅句柄
     */
    ProviderSubscription subscribe(ProviderSubscriptionRequest request);

    /**
     * 关闭 Provider 以及其持有的生产者和消费者资源。
     */
    @Override
    void close();
}
