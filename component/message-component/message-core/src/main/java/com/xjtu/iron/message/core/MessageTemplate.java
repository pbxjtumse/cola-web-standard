package com.xjtu.iron.message.core;

import com.xjtu.iron.message.core.codec.MessageWireCodec;
import com.xjtu.iron.message.core.context.CurrentMessage;
import com.xjtu.iron.message.core.context.MessageContextAccessor;
import com.xjtu.iron.message.core.context.ThreadLocalMessageContextAccessor;
import com.xjtu.iron.message.core.enrich.MessageEnvelopeEnricher;
import com.xjtu.iron.message.core.provider.MessageProviderRegistry;
import com.xjtu.iron.message.core.routing.DefaultDestinationResolver;
import com.xjtu.iron.message.core.routing.DestinationResolver;
import com.xjtu.iron.message.core.routing.DestinationRouteRegistry;
import com.xjtu.iron.message.core.send.DirectMessageSender;
import com.xjtu.iron.message.core.send.MessageSendExecutor;
import com.xjtu.iron.message.core.send.PreparedMessageSend;
import com.xjtu.iron.message.core.consume.ConsumeExecutionTemplate;

import com.xjtu.iron.message.api.consume.ConsumeDecision;
import com.xjtu.iron.message.api.consume.ConsumerDefinition;
import com.xjtu.iron.message.api.consume.MessageConsumerRegistrar;
import com.xjtu.iron.message.api.model.MessageDestination;
import com.xjtu.iron.message.api.model.MessageEnvelope;
import com.xjtu.iron.message.api.consume.MessageHandler;
import com.xjtu.iron.message.api.publish.MessagePublisher;
import com.xjtu.iron.foundation.id.api.StringIdGenerator;
import com.xjtu.iron.foundation.id.nanoid.NanoIdStringIdGenerator;
import com.xjtu.iron.foundation.serialization.Serializer;
import com.xjtu.iron.message.api.consume.MessageSubscription;
import com.xjtu.iron.message.api.publish.SendFailureType;
import com.xjtu.iron.message.api.publish.SendOptions;
import com.xjtu.iron.message.api.publish.SendReliabilityInfo;
import com.xjtu.iron.message.api.publish.SendResult;
import com.xjtu.iron.message.api.publish.SendStage;
import com.xjtu.iron.message.api.publish.SendStatus;
import com.xjtu.iron.message.spi.MessageCapability;
import com.xjtu.iron.message.spi.MessageProvider;
import com.xjtu.iron.message.spi.ProviderDestination;
import com.xjtu.iron.message.spi.ProviderInboundMessage;
import com.xjtu.iron.message.spi.ProviderSendRequest;
import com.xjtu.iron.message.spi.ProviderSubscription;
import com.xjtu.iron.message.spi.ProviderSubscriptionRequest;
import com.xjtu.iron.message.spi.ProviderConsumeResult;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
/**
 * message-component 的核心门面，统一承接业务侧的发送和订阅调用。
 *
 * <p>这个类的定位类似“组件编排器”：它不直接理解 Kafka、Pulsar、RocketMQ 的客户端细节，
 * 也不直接处理 retry-component 的内部算法，而是负责把一次业务调用拆成几个稳定阶段：</p>
 *
 * <ol>
 *   <li>校验逻辑目的地、消息信封和发送选项；</li>
 *   <li>通过 {@code MessageEnvelopeEnricher} 补齐 messageId、时间、上下文等公共元数据；</li>
 *   <li>通过 {@code DestinationResolver} 把逻辑目的地解析为 Provider 和物理 Topic；</li>
 *   <li>通过 {@code MessageProviderRegistry} 获取目标 Provider，并检查其发布/消费能力；</li>
 *   <li>通过 {@code MessageWireCodec} 把统一消息模型转换成 Provider 请求；</li>
 *   <li>把已经准备好的 {@code PreparedMessageSend} 交给 {@code MessageSendExecutor} 执行。</li>
 * </ol>
 *
 * <p>二期可靠发送之后，{@code MessageTemplate} 不再直接等待 Provider 的发送结果，
 * 这样可以保证主流程职责稳定：Template 只负责“准备发送”，可靠发送、重试、UNKNOWN 语义由 send 包处理。</p>
 */
public final class MessageTemplate
        implements MessagePublisher, MessageConsumerRegistrar, AutoCloseable {

    /** 组件运行参数。 */
    private final MessageComponentOptions options;

    /** Provider 注册表。 */
    private final MessageProviderRegistry providerRegistry;

    /** 逻辑目的地解析器。 */
    private final DestinationResolver destinationResolver;

    /** 消息信封丰富器。 */
    private final MessageEnvelopeEnricher envelopeEnricher;

    /** 线级消息映射器。 */
    private final MessageWireCodec wireCodec;

    /** 当前消息上下文访问器。 */
    private final MessageContextAccessor contextAccessor;

    /** 发送执行器。 */
    private final MessageSendExecutor sendExecutor;

    /** 消费执行模板。 */
    private final ConsumeExecutionTemplate consumeExecutionTemplate;

    public MessageTemplate(
            MessageComponentOptions options,
            MessageProviderRegistry providerRegistry,
            DestinationResolver destinationResolver,
            MessageEnvelopeEnricher envelopeEnricher,
            MessageWireCodec wireCodec,
            MessageContextAccessor contextAccessor,
            MessageSendExecutor sendExecutor) {
        this.options = Objects.requireNonNull(options, "options must not be null");
        this.providerRegistry = Objects.requireNonNull(providerRegistry, "providerRegistry must not be null");
        this.destinationResolver = Objects.requireNonNull(destinationResolver, "destinationResolver must not be null");
        this.envelopeEnricher = Objects.requireNonNull(envelopeEnricher, "envelopeEnricher must not be null");
        this.wireCodec = Objects.requireNonNull(wireCodec, "wireCodec must not be null");
        this.contextAccessor = Objects.requireNonNull(contextAccessor, "contextAccessor must not be null");
        this.sendExecutor = Objects.requireNonNull(sendExecutor, "sendExecutor must not be null");
        this.consumeExecutionTemplate = new ConsumeExecutionTemplate();
    }

    /**
     * 兼容一期的构造器。
     */
    public MessageTemplate(
            MessageComponentOptions options,
            MessageProviderRegistry providerRegistry,
            DestinationResolver destinationResolver,
            MessageEnvelopeEnricher envelopeEnricher,
            MessageWireCodec wireCodec,
            MessageContextAccessor contextAccessor) {
        this(
                options,
                providerRegistry,
                destinationResolver,
                envelopeEnricher,
                wireCodec,
                contextAccessor,
                new DirectMessageSender(options.clock()));
    }

    /**
     * 创建默认 MessageTemplate。
     */
    public static MessageTemplate create(
            MessageComponentOptions options,
            MessageProviderRegistry providerRegistry,
            DestinationRouteRegistry routeRegistry,
            Serializer payloadSerializer) {
        return create(
                options,
                providerRegistry,
                routeRegistry,
                payloadSerializer,
                new NanoIdStringIdGenerator(),
                new DirectMessageSender(options.clock()));
    }

    /**
     * 创建指定发送执行器的 MessageTemplate。
     */
    public static MessageTemplate create(
            MessageComponentOptions options,
            MessageProviderRegistry providerRegistry,
            DestinationRouteRegistry routeRegistry,
            Serializer payloadSerializer,
            MessageSendExecutor sendExecutor) {
        return create(
                options,
                providerRegistry,
                routeRegistry,
                payloadSerializer,
                new NanoIdStringIdGenerator(),
                sendExecutor);
    }

    /**
     * 创建指定消息 ID 生成器和发送执行器的 MessageTemplate。
     */
    public static MessageTemplate create(
            MessageComponentOptions options,
            MessageProviderRegistry providerRegistry,
            DestinationRouteRegistry routeRegistry,
            Serializer payloadSerializer,
            StringIdGenerator messageIdGenerator,
            MessageSendExecutor sendExecutor) {
        ThreadLocalMessageContextAccessor contextAccessor = new ThreadLocalMessageContextAccessor();
        DefaultDestinationResolver destinationResolver = new DefaultDestinationResolver(
                routeRegistry,
                options.defaultProviderName(),
                options.routingMode());
        MessageEnvelopeEnricher envelopeEnricher = new MessageEnvelopeEnricher(
                options,
                messageIdGenerator,
                contextAccessor);
        MessageWireCodec wireCodec = new MessageWireCodec(payloadSerializer);
        return new MessageTemplate(
                options,
                providerRegistry,
                destinationResolver,
                envelopeEnricher,
                wireCodec,
                contextAccessor,
                sendExecutor);
    }

    @Override
    public SendResult send(
            MessageDestination destination,
            MessageEnvelope<?> message,
            SendOptions options) {
        // 发送结果需要记录开始和结束时间，所以入口处先固定 startedAt，避免后续异常分支丢失时间信息。
        Instant startedAt = this.options.clock().instant();
        PreparedMessageSend prepared;
        try {
            // prepare 阶段只做发送前置工作：校验、补齐、路由、Provider 选择和编码，不真正触发中间件发送。
            prepared = prepare(destination, message, options, startedAt);
        } catch (PreparationException exception) {
            return preparationFailure(destination, message, startedAt, exception);
        }
        // 真正的发送交给 MessageSendExecutor。这里可能是 DirectMessageSender，也可能是可靠发送实现。
        return sendExecutor.send(prepared);
    }

    @Override
    public CompletionStage<SendResult> sendAsync(
            MessageDestination destination,
            MessageEnvelope<?> message,
            SendOptions options) {
        // 发送结果需要记录开始和结束时间，所以入口处先固定 startedAt，避免后续异常分支丢失时间信息。
        Instant startedAt = this.options.clock().instant();
        PreparedMessageSend prepared;
        try {
            // prepare 阶段只做发送前置工作：校验、补齐、路由、Provider 选择和编码，不真正触发中间件发送。
            prepared = prepare(destination, message, options, startedAt);
        } catch (PreparationException exception) {
            return CompletableFuture.completedFuture(
                    preparationFailure(destination, message, startedAt, exception));
        }
        return sendExecutor.sendAsync(prepared);
    }

    @Override
    public <T> MessageSubscription subscribe(
            ConsumerDefinition<T> definition,
            MessageHandler<T> handler) {
        Objects.requireNonNull(definition, "definition must not be null");
        Objects.requireNonNull(handler, "handler must not be null");
        ProviderDestination providerDestination = destinationResolver.resolve(definition.destination());
        MessageProvider provider = providerRegistry.getRequired(providerDestination.providerName());
        requireCapability(provider, MessageCapability.BASIC_CONSUME);
        ProviderSubscriptionRequest providerRequest = new ProviderSubscriptionRequest(
                providerDestination,
                definition.consumerGroup(),
                inbound -> ProviderConsumeResult.of(handleInbound(definition, handler, providerDestination, inbound)));
        ProviderSubscription providerSubscription = provider.subscribe(providerRequest);
        if (providerSubscription == null) {
            throw new IllegalStateException("provider returned null subscription");
        }
        return providerSubscription::close;
    }

    @Override
    public void close() {
        providerRegistry.close();
    }

    /**
     * 准备一次发送操作。
     */
    private PreparedMessageSend prepare(
            MessageDestination destination,
            MessageEnvelope<?> message,
            SendOptions sendOptions,
            Instant startedAt) {
        if (destination == null) {
            throw new PreparationException(
                    SendStage.VALIDATE,
                    SendFailureType.VALIDATION_ERROR,
                    "destination must not be null",
                    null,
                    null,
                    null);
        }
        if (message == null) {
            throw new PreparationException(
                    SendStage.VALIDATE,
                    SendFailureType.VALIDATION_ERROR,
                    "message must not be null",
                    null,
                    null,
                    null);
        }
        // 业务可以不传 SendOptions，此时使用组件默认确认超时和默认发送语义。
        SendOptions actualOptions = sendOptions == null ? SendOptions.defaults() : sendOptions;
        // confirmTimeout 是“单次 Provider 发送等待确认”的超时，不是整个 retry 的最大时长。
        Duration confirmTimeout = actualOptions.confirmTimeout() == null
                ? options.defaultConfirmTimeout()
                : actualOptions.confirmTimeout();
        MessageEnvelope<?> enrichedMessage;
        try {
            // 补齐 messageId、createdAt、source、correlationId 等公共元数据，保证后续 Provider 不再处理这些公共字段。
            enrichedMessage = envelopeEnricher.enrich(message);
        } catch (RuntimeException exception) {
            throw new PreparationException(
                    SendStage.ENRICH,
                    SendFailureType.VALIDATION_ERROR,
                    exception.getMessage(),
                    exception,
                    null,
                    null);
        }
        ProviderDestination providerDestination;
        try {
            // 将 API 层逻辑目的地解析为 SPI 层物理目的地，例如 demo:message -> message-demo-topic。
            providerDestination = destinationResolver.resolve(destination);
        } catch (RuntimeException exception) {
            throw new PreparationException(
                    SendStage.RESOLVE,
                    SendFailureType.ROUTING_ERROR,
                    exception.getMessage(),
                    exception,
                    enrichedMessage,
                    null);
        }
        MessageProvider provider;
        try {
            provider = providerRegistry.getRequired(providerDestination.providerName());
        } catch (RuntimeException exception) {
            throw new PreparationException(
                    SendStage.RESOLVE,
                    SendFailureType.PROVIDER_NOT_FOUND,
                    exception.getMessage(),
                    exception,
                    enrichedMessage,
                    providerDestination);
        }
        if (!provider.capabilities().contains(MessageCapability.BASIC_PUBLISH)) {
            throw new PreparationException(
                    SendStage.RESOLVE,
                    SendFailureType.UNSUPPORTED_CAPABILITY,
                    "provider does not support BASIC_PUBLISH: " + provider.name(),
                    null,
                    enrichedMessage,
                    providerDestination);
        }
        ProviderSendRequest request;
        try {
            // wire codec 把统一消息模型转换为 Provider 请求，payload 序列化和系统 header 都在这里完成。
            request = wireCodec.encode(destination, providerDestination, enrichedMessage);
        } catch (RuntimeException exception) {
            throw new PreparationException(
                    SendStage.SERIALIZE,
                    SendFailureType.SERIALIZATION_ERROR,
                    exception.getMessage(),
                    exception,
                    enrichedMessage,
                    providerDestination);
        }
        return new PreparedMessageSend(
                destination,
                enrichedMessage,
                providerDestination,
                provider,
                request,
                confirmTimeout,
                startedAt);
    }

    @SuppressWarnings("try")
    private <T> ConsumeDecision handleInbound(
            ConsumerDefinition<T> definition,
            MessageHandler<T> handler,
            ProviderDestination providerDestination,
            ProviderInboundMessage inbound) {
        try {
            MessageWireCodec.DecodedInbound<T> decoded = wireCodec.decode(
                    definition,
                    providerDestination,
                    inbound);
            try (MessageContextAccessor.Scope ignored = contextAccessor.open(
                    new CurrentMessage(decoded.envelope(), decoded.consumeContext()))) {
                return consumeExecutionTemplate.execute(definition, decoded.envelope(), decoded.consumeContext(), handler);
            }
        } catch (RuntimeException exception) {
            return ConsumeDecision.RETRY;
        }
    }

    private static void requireCapability(
            MessageProvider provider,
            MessageCapability capability) {
        if (!provider.capabilities().contains(capability)) {
            throw new IllegalStateException(
                    "provider does not support " + capability + ": " + provider.name());
        }
    }

    private SendResult preparationFailure(
            MessageDestination destination,
            MessageEnvelope<?> originalMessage,
            Instant startedAt,
            PreparationException exception) {
        String messageId = exception.enrichedMessage() != null
                ? exception.enrichedMessage().messageId()
                : originalMessage == null ? null : originalMessage.messageId();
        String providerName = exception.providerDestination() == null
                ? destination == null ? null : destination.providerHint()
                : exception.providerDestination().providerName();
        String physicalName = exception.providerDestination() == null
                ? null
                : exception.providerDestination().physicalName();
        SendStatus status = exception.stage() == SendStage.VALIDATE
                || exception.stage() == SendStage.RESOLVE
                ? SendStatus.REJECTED
                : SendStatus.FAILED;
        return new SendResult(
                messageId,
                destination,
                providerName,
                physicalName,
                status,
                exception.stage(),
                exception.failureType(),
                null,
                exception.getMessage(),
                startedAt,
                options.clock().instant(),
                Map.of(),
                SendReliabilityInfo.disabled());
    }

    /**
     * 携带准备阶段和部分准备结果的内部异常。
     */
    @SuppressWarnings("serial")
    private static final class PreparationException extends RuntimeException {

        /** 失败阶段。 */
        private final SendStage stage;

        /** 标准失败类型。 */
        private final SendFailureType failureType;

        /** 已丰富消息；丰富前失败时为空。 */
        private final MessageEnvelope<?> enrichedMessage;

        /** 已解析物理目的地；路由前失败时为空。 */
        private final ProviderDestination providerDestination;

        private PreparationException(
                SendStage stage,
                SendFailureType failureType,
                String message,
                Throwable cause,
                MessageEnvelope<?> enrichedMessage,
                ProviderDestination providerDestination) {
            super(message == null ? failureType.name() : message, cause);
            this.stage = stage;
            this.failureType = failureType;
            this.enrichedMessage = enrichedMessage;
            this.providerDestination = providerDestination;
        }

        private SendStage stage() {
            return stage;
        }

        private SendFailureType failureType() {
            return failureType;
        }

        private MessageEnvelope<?> enrichedMessage() {
            return enrichedMessage;
        }

        private ProviderDestination providerDestination() {
            return providerDestination;
        }
    }
}
