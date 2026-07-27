package com.xjtu.iron.message.demo;

import com.xjtu.iron.message.api.ConsumerDefinition;
import com.xjtu.iron.message.api.MessageContext;
import com.xjtu.iron.message.api.MessageDestination;
import com.xjtu.iron.message.api.MessageEnvelope;
import com.xjtu.iron.message.api.MessageHeaderNames;
import com.xjtu.iron.message.api.SendFailureType;
import com.xjtu.iron.message.api.SendResult;
import com.xjtu.iron.message.api.SendStatus;
import com.xjtu.iron.message.core.DestinationRoute;
import com.xjtu.iron.message.core.DestinationRouteRegistry;
import com.xjtu.iron.message.core.MessageComponentOptions;
import com.xjtu.iron.message.core.MessageProviderRegistry;
import com.xjtu.iron.message.core.MessageTemplate;
import com.xjtu.iron.message.core.MessageWireCodec;
import com.xjtu.iron.message.spi.ProviderDestination;
import com.xjtu.iron.message.spi.ProviderInboundMessage;
import com.xjtu.iron.message.spi.ProviderSendRequest;
import com.xjtu.iron.message.testkit.InMemoryMessageProvider;
import com.xjtu.iron.message.testkit.InMemoryMessageRecord;
import com.xjtu.iron.message.testkit.Utf8StringMessageSerializer;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** 使用无第三方依赖的内存 Provider 验证公共模型不变量。 */
public final class MessageModelContractVerifier {

    private MessageModelContractVerifier() {
    }

    public static void main(String[] args) {
        verifyMessageIdAndMessageKeyAreDifferentConcepts();
        verifyOptionalSourceAndDefaultCorrelation();
        verifyStrictRouting();
        verifyReservedHeaderProtection();
        verifyLogicalDestinationValidation();
        System.out.println("message model contract verification=PASSED");
    }

    private static void verifyMessageIdAndMessageKeyAreDifferentConcepts() {
        MessageEnvelope<String> first = MessageEnvelope.builder("OrderCreated", "payload-1")
                .messageId("message-1")
                .messageKey("order-10001")
                .build();
        MessageEnvelope<String> second = MessageEnvelope.builder("OrderPaid", "payload-2")
                .messageId("message-2")
                .messageKey("order-10001")
                .build();
        require(!first.messageId().equals(second.messageId()), "different messages need different messageId");
        require(first.messageKey().equals(second.messageKey()), "same aggregate may share messageKey");
    }

    private static void verifyOptionalSourceAndDefaultCorrelation() {
        MessageDestination destination = MessageDestination.of("trade", "order-created");
        InMemoryMessageProvider provider = new InMemoryMessageProvider();
        MessageTemplate template = MessageTemplate.create(
                MessageComponentOptions.defaults(provider.name(), null),
                new MessageProviderRegistry(List.of(provider)),
                new DestinationRouteRegistry(List.of(
                        DestinationRoute.of(destination, provider.name(), "trade-order-created-topic"))),
                new Utf8StringMessageSerializer());
        SendResult result = template.send(
                destination,
                MessageEnvelope.builder("OrderCreated", "orderId=20001")
                        .messageKey("order-20001")
                        .context(MessageContext.empty())
                        .build());
        require(result.status() == SendStatus.CONFIRMED, "root message must be confirmed");
        InMemoryMessageRecord record = provider.records().get(0);
        require(
                record.headers().get(MessageHeaderNames.MESSAGE_ID)
                        .equals(record.headers().get(MessageHeaderNames.CORRELATION_ID)),
                "root correlationId must default to messageId");
        require(!record.headers().containsKey(MessageHeaderNames.SOURCE), "source must remain absent");
        template.close();
    }

    private static void verifyStrictRouting() {
        InMemoryMessageProvider provider = new InMemoryMessageProvider();
        MessageTemplate template = MessageTemplate.create(
                MessageComponentOptions.defaults(provider.name(), "trade-service"),
                new MessageProviderRegistry(List.of(provider)),
                DestinationRouteRegistry.empty(),
                new Utf8StringMessageSerializer());
        SendResult result = template.send(
                MessageDestination.of("trade", "route-not-configured"),
                MessageEnvelope.of("RouteNotConfigured", "payload"));
        require(result.status() == SendStatus.REJECTED, "strict route must be rejected");
        require(result.failureType() == SendFailureType.ROUTING_ERROR, "failure must be ROUTING_ERROR");
        template.close();
    }

    private static void verifyReservedHeaderProtection() {
        boolean rejected = false;
        try {
            MessageEnvelope.builder("ReservedHeader", "payload")
                    .header(MessageHeaderNames.MESSAGE_ID, "fake-message-id")
                    .build();
        } catch (IllegalArgumentException expected) {
            rejected = true;
        }
        require(rejected, "reserved system header must be rejected");
    }

    private static void verifyLogicalDestinationValidation() {
        MessageDestination actual = MessageDestination.of("trade", "order-paid");
        MessageDestination wrong = MessageDestination.of("trade", "order-cancelled");
        MessageWireCodec codec = new MessageWireCodec(new Utf8StringMessageSerializer());
        MessageEnvelope<String> envelope = MessageEnvelope.builder("OrderPaid", "orderId=30001")
                .messageId("message-30001")
                .schemaVersion("1")
                .messageKey("order-30001")
                .context(MessageContext.builder().correlationId("order-30001").build())
                .occurredAt(Instant.parse("2026-07-26T00:00:00Z"))
                .createdAt(Instant.parse("2026-07-26T00:00:00Z"))
                .build();
        ProviderDestination providerDestination = new ProviderDestination(
                "memory",
                "shared-physical-topic",
                Map.of());
        ProviderSendRequest request = codec.encode(actual, providerDestination, envelope);
        ProviderInboundMessage inbound = new ProviderInboundMessage(
                "provider-message-30001",
                request.messageKey(),
                request.headers(),
                request.body(),
                Instant.parse("2026-07-26T00:00:01Z"),
                Map.of());
        boolean rejected = false;
        try {
            codec.decode(
                    ConsumerDefinition.of(wrong, "wrong-consumer", String.class),
                    providerDestination,
                    inbound);
        } catch (IllegalArgumentException expected) {
            rejected = true;
        }
        require(rejected, "logical destination mismatch must be rejected");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
