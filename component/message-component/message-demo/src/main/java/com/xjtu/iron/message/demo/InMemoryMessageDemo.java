package com.xjtu.iron.message.demo;

import com.xjtu.iron.message.api.ConsumeDecision;
import com.xjtu.iron.message.api.ConsumerDefinition;
import com.xjtu.iron.message.api.MessageConsumer;
import com.xjtu.iron.message.api.MessageDestination;
import com.xjtu.iron.message.api.MessageEnvelope;
import com.xjtu.iron.message.api.SendOptions;
import com.xjtu.iron.message.api.SendResult;
import com.xjtu.iron.message.core.MessageProviderRegistry;
import com.xjtu.iron.message.core.MessageTemplate;
import com.xjtu.iron.message.testkit.InMemoryMessageProvider;
import com.xjtu.iron.message.testkit.StringMessageSerializer;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 演示第一版普通消息发送和消费基础闭环。
 */
public final class InMemoryMessageDemo {

    /**
     * 启动可直接运行的内存示例。
     *
     * @param args 命令行参数；当前示例不使用
     * @throws InterruptedException 等待消费结果被中断时抛出
     */
    public static void main(String[] args) throws InterruptedException {
        // 创建不依赖真实 MQ 的内存 Provider。
        InMemoryMessageProvider provider = new InMemoryMessageProvider();
        // 创建 Provider 注册表。
        MessageProviderRegistry registry = new MessageProviderRegistry(List.of(provider));
        // 创建核心消息门面。
        MessageTemplate template = new MessageTemplate(
                registry,
                new StringMessageSerializer(),
                "message-demo");
        // 定义使用内存 Provider 的逻辑 Topic。
        MessageDestination destination = MessageDestination.of(
                InMemoryMessageProvider.NAME,
                "order-paid");
        // 创建等待一次成功消费的同步器。
        CountDownLatch consumed = new CountDownLatch(1);
        // 注册业务消费者。
        MessageConsumer consumer = template.subscribe(new ConsumerDefinition<>(
                destination,
                "order-service",
                String.class,
                (payload, context) -> {
                    // 打印本次消费的核心信息。
                    System.out.println(
                            "consume payload=" + payload
                                    + ", attempt=" + context.deliveryAttempt()
                                    + ", provider=" + context.providerName());
                    // 通知主线程消费已经完成。
                    consumed.countDown();
                    // 返回成功决策，允许 Provider ACK。
                    return ConsumeDecision.SUCCESS;
                }));
        // 创建业务消息并指定订单号作为消息键。
        MessageEnvelope<String> message = MessageEnvelope
                .of("OrderPaid", "orderId=10001, amount=99.00")
                .withKey("10001");
        // 同步发送并等待明确确认。
        SendResult sendResult = template.send(
                destination,
                message,
                SendOptions.defaults());
        // 打印标准发送结果。
        System.out.println(
                "send status=" + sendResult.status()
                        + ", messageId=" + sendResult.messageId()
                        + ", nativeMessageId=" + sendResult.nativeMessageId());
        // 最多等待两秒，验证消息确实完成消费闭环。
        boolean completed = consumed.await(2, TimeUnit.SECONDS);
        // 未完成时让示例明确失败。
        if (!completed) {
            // 抛出异常，避免演示静默结束。
            throw new IllegalStateException("message was not consumed within timeout");
        }
        // 关闭消费者。
        consumer.close();
        // 关闭模板及全部 Provider。
        template.close();
    }

    /**
     * 禁止实例化示例入口类。
     */
    private InMemoryMessageDemo() {
        // 示例只通过 main 方法运行。
    }
}
