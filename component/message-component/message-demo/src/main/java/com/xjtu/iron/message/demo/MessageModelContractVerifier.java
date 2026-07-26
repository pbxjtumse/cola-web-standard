package com.xjtu.iron.message.demo;

import com.xjtu.iron.message.api.MessageContext;
import com.xjtu.iron.message.api.MessageDestination;
import com.xjtu.iron.message.api.MessageEnvelope;
import com.xjtu.iron.message.api.SendOptions;
import com.xjtu.iron.message.api.SendResult;
import com.xjtu.iron.message.api.SendStatus;
import com.xjtu.iron.message.core.DestinationRouteRegistry;
import com.xjtu.iron.message.core.MessageComponentOptions;
import com.xjtu.iron.message.core.MessageProviderRegistry;
import com.xjtu.iron.message.core.MessageTemplate;
import com.xjtu.iron.message.testkit.InMemoryMessageProvider;
import com.xjtu.iron.message.testkit.InMemoryMessageRecord;
import com.xjtu.iron.message.testkit.Utf8StringMessageSerializer;

import java.util.List;

/**
 * 使用无第三方依赖的内存 Provider 验证第一版公共模型的重要不变量。
 */
public final class MessageModelContractVerifier {

    /**
     * 工具类不允许实例化。
     */
    private MessageModelContractVerifier() {
        // 私有构造器阻止无意义实例创建。
    }

    /**
     * 执行公共模型契约验证。
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        // 验证 source 不会在没有配置时被虚构。
        verifyOptionalSourceAndDefaultCorrelation();
        // 验证严格路由不会自动生成生产 Topic。
        verifyStrictRouting();
        // 验证业务不能伪造组件系统头。
        verifyReservedHeaderProtection();
        // 验证入站逻辑目的地不能与消费者契约串线。
        verifyLogicalDestinationValidation();
        // 输出成功标记，供脚本和 CI 判断。
        System.out.println("message model contract verification=PASSED");
    }

    /**
     * 验证根消息默认 correlationId 和可选 source。
     */
    private static void verifyOptionalSourceAndDefaultCorrelation() {
        // 创建逻辑事件目的地。
        MessageDestination destination = MessageDestination.event("trade", "order-created");
        // 创建内存 Provider。
        InMemoryMessageProvider provider = new InMemoryMessageProvider();
        // 创建 Provider 注册表。
        MessageProviderRegistry providerRegistry = new MessageProviderRegistry(List.of(provider));
        // 使用空路由表配合显式 providerHint，仅用于验证路由错误分支之外的模型行为。
        DestinationRouteRegistry routeRegistry = new DestinationRouteRegistry(List.of(
                com.xjtu.iron.message.core.DestinationRoute.of(
                        destination,
                        provider.name(),
                        "trade-order-created-topic")));
        // applicationName 传 null，验证 source 不会被强行生成。
        MessageComponentOptions options = MessageComponentOptions.defaults(provider.name(), null);
        // 创建消息模板。
        MessageTemplate template = MessageTemplate.create(
                options,
                providerRegistry,
                routeRegistry,
                new Utf8StringMessageSerializer());
        // 构造没有 messageId、source 和 correlationId 的根消息。
        MessageEnvelope<String> message = MessageEnvelope.builder(
                        "OrderCreated",
                        "orderId=20001")
                // 显式传入空上下文以验证 core 补齐规则。
                .context(MessageContext.empty())
                // 完成构造。
                .build();
        // 发送消息。
        SendResult result = template.send(destination, message, SendOptions.defaults());
        // 发送必须获得明确确认。
        require(result.status() == SendStatus.CONFIRMED, "root message must be confirmed");
        // 获取 Provider 实际记录。
        InMemoryMessageRecord record = provider.records().get(0);
        // 根消息的 correlationId 应默认等于生成后的 messageId。
        require(
                record.headers().get(com.xjtu.iron.message.api.MessageHeaders.MESSAGE_ID)
                        .equals(record.headers().get(
                                com.xjtu.iron.message.api.MessageHeaders.CORRELATION_ID)),
                "root correlationId must default to messageId");
        // 未显式设置且 applicationName 为空时不应出现 source 系统头。
        require(
                !record.headers().containsKey(
                        com.xjtu.iron.message.api.MessageHeaders.SOURCE),
                "source must remain absent when not configured");
        // 关闭模板和 Provider。
        template.close();
    }

    /**
     * 验证严格路由模式在缺少精确路由时返回路由失败。
     */
    private static void verifyStrictRouting() {
        // 创建内存 Provider。
        InMemoryMessageProvider provider = new InMemoryMessageProvider();
        // 创建仅包含 Provider 的注册表。
        MessageProviderRegistry providerRegistry = new MessageProviderRegistry(List.of(provider));
        // 默认配置使用 STRICT 路由模式。
        MessageComponentOptions options = MessageComponentOptions.defaults(
                provider.name(),
                "trade-service");
        // 使用空路由表创建模板。
        MessageTemplate template = MessageTemplate.create(
                options,
                providerRegistry,
                DestinationRouteRegistry.empty(),
                new Utf8StringMessageSerializer());
        // 构造一个没有配置路由的逻辑目的地。
        MessageDestination missingDestination = MessageDestination.event(
                "trade",
                "route-not-configured");
        // 尝试发送消息。
        SendResult result = template.send(
                missingDestination,
                MessageEnvelope.of("RouteNotConfigured", "payload"),
                SendOptions.defaults());
        // 准备阶段必须明确失败，而不能自动创建或猜测 Topic。
        require(result.status() == SendStatus.REJECTED, "strict route must be rejected");
        // 失败类型必须是路由错误。
        require(
                result.failureType()
                        == com.xjtu.iron.message.api.SendFailureType.ROUTING_ERROR,
                "strict route failure type must be ROUTING_ERROR");
        // 关闭模板和 Provider。
        template.close();
    }


    /**
     * 验证用户消息头不能覆盖组件保留系统头。
     */
    private static void verifyReservedHeaderProtection() {
        // 记录是否捕获到预期参数异常。
        boolean rejected = false;
        // 尝试伪造系统消息 ID。
        try {
            // Builder 应在写入时立即拒绝保留前缀。
            MessageEnvelope.builder("ReservedHeader", "payload")
                    .header(
                            com.xjtu.iron.message.api.MessageHeaders.MESSAGE_ID,
                            "fake-message-id")
                    .build();
        } catch (IllegalArgumentException expected) {
            // 捕获预期异常。
            rejected = true;
        }
        // 未拒绝意味着业务可以覆盖组件系统元数据。
        require(rejected, "reserved system header must be rejected");
    }

    /**
     * 验证线级逻辑目的地与消费者定义必须一致。
     */
    private static void verifyLogicalDestinationValidation() {
        // 定义真实发送目的地。
        MessageDestination actualDestination = MessageDestination.event(
                "trade",
                "order-paid");
        // 定义错误消费者目的地。
        MessageDestination wrongDestination = MessageDestination.event(
                "trade",
                "order-cancelled");
        // 创建线级映射器。
        com.xjtu.iron.message.core.MessageWireMapper wireMapper =
                new com.xjtu.iron.message.core.MessageWireMapper(
                        new Utf8StringMessageSerializer());
        // 创建已经补齐稳定字段的消息。
        MessageEnvelope<String> envelope = MessageEnvelope.builder(
                        "OrderPaid",
                        "orderId=30001")
                .messageId("message-30001")
                .schemaVersion("1")
                .context(MessageContext.builder()
                        .correlationId("order-30001")
                        .build())
                .occurredAt(java.time.Instant.parse("2026-07-26T00:00:00Z"))
                .createdAt(java.time.Instant.parse("2026-07-26T00:00:00Z"))
                .build();
        // 创建测试物理目的地。
        com.xjtu.iron.message.spi.ProviderDestination providerDestination =
                new com.xjtu.iron.message.spi.ProviderDestination(
                        "memory",
                        "shared-physical-topic",
                        java.util.Map.of());
        // 将正确逻辑目的地编码到线级请求。
        com.xjtu.iron.message.spi.ProviderSendRequest request =
                wireMapper.toProviderRequest(
                        actualDestination,
                        providerDestination,
                        envelope);
        // 构造模拟入站消息。
        com.xjtu.iron.message.spi.ProviderInboundMessage inbound =
                new com.xjtu.iron.message.spi.ProviderInboundMessage(
                        "provider-message-30001",
                        request.key(),
                        request.headers(),
                        request.body(),
                        1,
                        java.time.Instant.parse("2026-07-26T00:00:01Z"),
                        java.util.Map.of());
        // 记录错误消费者是否被拒绝。
        boolean rejected = false;
        // 使用错误逻辑目的地解码。
        try {
            // 消费者定义与线级目的地不一致时必须失败。
            wireMapper.fromProviderMessage(
                    com.xjtu.iron.message.api.ConsumerDefinition.of(
                            wrongDestination,
                            "wrong-consumer",
                            String.class),
                    providerDestination,
                    inbound);
        } catch (IllegalArgumentException expected) {
            // 捕获预期异常。
            rejected = true;
        }
        // 未拒绝意味着不同消息契约可能在同一物理 Topic 上串线。
        require(rejected, "logical destination mismatch must be rejected");
    }

    /**
     * 断言条件成立。
     *
     * @param condition 条件
     * @param message 失败描述
     */
    private static void require(boolean condition, String message) {
        // 条件不成立时终止验证。
        if (!condition) {
            // 抛出明确的契约验证异常。
            throw new IllegalStateException(message);
        }
    }
}
