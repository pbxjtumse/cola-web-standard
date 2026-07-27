package com.xjtu.iron.message.demo;

import com.xjtu.iron.message.api.ConsumeDecision;
import com.xjtu.iron.message.api.ConsumerDefinition;
import com.xjtu.iron.message.api.MessageContext;
import com.xjtu.iron.message.api.MessageDestination;
import com.xjtu.iron.message.api.MessageEnvelope;
import com.xjtu.iron.message.api.MessageSubscription;
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

/** 演示逻辑路由、messageKey 和父子消息关联传播。 */
public final class InMemoryMessageDemo {

    private InMemoryMessageDemo() {
    }

    public static void main(String[] args) throws Exception {
        MessageDestination orderPaid = MessageDestination.of("trade", "order-paid");
        MessageDestination pointsGranted = MessageDestination.of("member", "points-granted");
        InMemoryMessageProvider provider = new InMemoryMessageProvider();
        MessageProviderRegistry providers = new MessageProviderRegistry(List.of(provider));
        DestinationRouteRegistry routes = new DestinationRouteRegistry(List.of(
                DestinationRoute.of(orderPaid, provider.name(), "trade-order-paid-topic"),
                DestinationRoute.of(pointsGranted, provider.name(), "member-points-topic")));
        MessageTemplate template = MessageTemplate.create(
                MessageComponentOptions.defaults(provider.name(), "order-service"),
                providers,
                routes,
                new Utf8StringMessageSerializer());
        CountDownLatch completed = new CountDownLatch(1);

        MessageSubscription pointsSubscription = template.subscribe(
                ConsumerDefinition.of(pointsGranted, "member-points-consumer", String.class),
                (message, context) -> {
                    System.out.println("points messageId=" + message.messageId());
                    System.out.println("points messageKey=" + message.messageKey());
                    System.out.println("points correlationId=" + message.context().correlationId());
                    System.out.println("points causationId=" + message.context().causationId());
                    completed.countDown();
                    return ConsumeDecision.SUCCESS;
                });

        MessageSubscription orderSubscription = template.subscribe(
                ConsumerDefinition.of(orderPaid, "trade-order-consumer", String.class),
                (message, context) -> {
                    System.out.println("order messageId=" + message.messageId());
                    System.out.println("order messageKey=" + message.messageKey());
                    SendResult child = template.send(
                            pointsGranted,
                            MessageEnvelope.builder("PointsGranted", "orderId=10001, points=99")
                                    .messageKey("order-10001")
                                    .build());
                    System.out.println("child send status=" + child.status());
                    return ConsumeDecision.SUCCESS;
                });

        MessageEnvelope<String> root = MessageEnvelope.builder(
                        "OrderPaid",
                        "orderId=10001, amount=99.00")
                .messageKey("order-10001")
                .context(MessageContext.builder().correlationId("order-flow-10001").build())
                .header("biz-scene", "normal-payment")
                .build();
        SendResult rootResult = template.send(orderPaid, root);
        System.out.println("root send status=" + rootResult.status());

        if (!completed.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("demo consumer timeout");
        }
        orderSubscription.close();
        pointsSubscription.close();
        template.close();
    }
}
