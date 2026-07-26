package com.xjtu.iron.message.demo;

import com.xjtu.iron.message.api.ConsumeDecision;
import com.xjtu.iron.message.api.ConsumerDefinition;
import com.xjtu.iron.message.api.MessageContext;
import com.xjtu.iron.message.api.MessageDestination;
import com.xjtu.iron.message.api.MessageEnvelope;
import com.xjtu.iron.message.api.MessageSubscription;
import com.xjtu.iron.message.api.SendOptions;
import com.xjtu.iron.message.api.SendResult;
import com.xjtu.iron.message.core.DestinationRoute;
import com.xjtu.iron.message.core.DestinationRouteRegistry;
import com.xjtu.iron.message.core.MessageComponentOptions;
import com.xjtu.iron.message.core.MessageProviderRegistry;
import com.xjtu.iron.message.core.MessageTemplate;
import com.xjtu.iron.message.testkit.InMemoryMessageProvider;
import com.xjtu.iron.message.testkit.Utf8StringMessageSerializer;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 演示逻辑目的地路由、消息上下文和 causationId 自动传播。
 */
public final class InMemoryMessageDemo {

    /**
     * Demo 工具类不允许创建实例。
     */
    private InMemoryMessageDemo() {
        // 私有构造器阻止实例化。
    }

    /**
     * 运行内存基础闭环。
     *
     * @param args 命令行参数
     * @throws Exception 等待消费超时或线程中断
     */
    public static void main(String[] args) throws Exception {
        // 定义订单已支付事件逻辑目的地。
        MessageDestination orderPaid = MessageDestination.event(
                "trade",
                "order-paid");
        // 定义积分已发放事件逻辑目的地。
        MessageDestination pointsGranted = MessageDestination.event(
                "member",
                "points-granted");
        // 创建内存 Provider。
        InMemoryMessageProvider provider = new InMemoryMessageProvider();
        // 创建 Provider 注册表。
        MessageProviderRegistry providerRegistry = new MessageProviderRegistry(
                List.of(provider));
        // 显式配置两个逻辑目的地的物理路由。
        DestinationRouteRegistry routeRegistry = new DestinationRouteRegistry(List.of(
                DestinationRoute.of(orderPaid, provider.name(), "trade-order-paid-topic"),
                DestinationRoute.of(pointsGranted, provider.name(), "member-points-topic")));
        // 创建组件参数，applicationName 会作为默认 source。
        MessageComponentOptions options = MessageComponentOptions.defaults(
                provider.name(),
                "order-service");
        // 创建统一 MessageTemplate。
        MessageTemplate messageTemplate = MessageTemplate.create(
                options,
                providerRegistry,
                routeRegistry,
                new Utf8StringMessageSerializer());
        // 用于等待下游消息消费完成。
        CountDownLatch completed = new CountDownLatch(1);
        // 注册积分消息消费者。
        MessageSubscription pointsSubscription = messageTemplate.subscribe(
                ConsumerDefinition.of(
                        pointsGranted,
                        "member-points-consumer",
                        String.class),
                (message, context) -> {
                    // 输出下游消息关联信息。
                    System.out.println("points payload=" + message.payload());
                    // correlationId 应继承根消息的业务关联标识。
                    System.out.println("points correlationId="
                            + message.context().correlationId());
                    // causationId 应等于直接父消息 order-paid 的 messageId。
                    System.out.println("points causationId="
                            + message.context().causationId());
                    // 通知主线程消费完成。
                    completed.countDown();
                    // 返回成功确认。
                    return ConsumeDecision.SUCCESS;
                });
        // 注册订单支付消息消费者。
        MessageSubscription orderSubscription = messageTemplate.subscribe(
                ConsumerDefinition.of(
                        orderPaid,
                        "trade-order-consumer",
                        String.class),
                (message, context) -> {
                    // 输出根消息信息。
                    System.out.println("order messageId=" + message.messageId());
                    // 在当前消费上下文中发送下游消息，不显式设置 correlationId 和 causationId。
                    SendResult childResult = messageTemplate.send(
                            pointsGranted,
                            MessageEnvelope.builder(
                                            "PointsGranted",
                                            "orderId=10001, points=99")
                                    // 使用订单号作为下游分区键。
                                    .key("10001")
                                    // 构造下游消息。
                                    .build(),
                            SendOptions.defaults());
                    // 输出下游发送状态。
                    System.out.println("child send status=" + childResult.status());
                    // 返回成功确认根消息。
                    return ConsumeDecision.SUCCESS;
                });
        // 构造根消息，并显式用订单号作为业务流程 correlationId。
        MessageEnvelope<String> rootMessage = MessageEnvelope.builder(
                        "OrderPaid",
                        "orderId=10001, amount=99.00")
                // 使用订单号作为分区和定位键。
                .key("10001")
                // source 不显式设置，将由 applicationName 补齐。
                .context(MessageContext.builder()
                        // 整个订单流程使用订单号关联。
                        .correlationId("order-10001")
                        // 构造稳定上下文。
                        .build())
                // 增加用户消息头。
                .header("biz-scene", "normal-payment")
                // 构造根消息。
                .build();
        // 发送根消息。
        SendResult rootResult = messageTemplate.send(
                orderPaid,
                rootMessage,
                SendOptions.defaults());
        // 输出发送结果。
        System.out.println("root send status=" + rootResult.status());
        // 最多等待五秒。
        boolean finished = completed.await(5, TimeUnit.SECONDS);
        // 未完成时抛出错误。
        if (!finished) {
            // 明确提示 Demo 消费超时。
            throw new IllegalStateException("demo consumer timeout");
        }
        // 关闭两个订阅。
        orderSubscription.close();
        // 关闭积分订阅。
        pointsSubscription.close();
        // 关闭模板及 Provider。
        messageTemplate.close();
    }
}
