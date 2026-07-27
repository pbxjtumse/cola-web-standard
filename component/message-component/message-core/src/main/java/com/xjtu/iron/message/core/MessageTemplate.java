package com.xjtu.iron.message.core;

import com.xjtu.iron.message.api.ConsumeDecision;
import com.xjtu.iron.message.api.ConsumerDefinition;
import com.xjtu.iron.message.api.MessageConsumerRegistrar;
import com.xjtu.iron.message.api.MessageDestination;
import com.xjtu.iron.message.api.MessageEnvelope;
import com.xjtu.iron.message.api.MessageHandler;
import com.xjtu.iron.message.api.MessagePublisher;
import com.xjtu.iron.message.api.MessageSubscription;
import com.xjtu.iron.message.api.SendFailureType;
import com.xjtu.iron.message.api.SendOptions;
import com.xjtu.iron.message.api.SendResult;
import com.xjtu.iron.message.api.SendStage;
import com.xjtu.iron.message.api.SendStatus;
import com.xjtu.iron.message.spi.MessageCapability;
import com.xjtu.iron.message.spi.MessageProvider;
import com.xjtu.iron.message.spi.ProviderDestination;
import com.xjtu.iron.message.spi.ProviderInboundMessage;
import com.xjtu.iron.message.spi.ProviderSendRequest;
import com.xjtu.iron.message.spi.ProviderSendResult;
import com.xjtu.iron.message.spi.ProviderSubscription;
import com.xjtu.iron.message.spi.ProviderSubscriptionRequest;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 统一编排普通消息发送和消费生命周期的核心入口。
 *
 * <p>当前只有一种生命周期编排，因此第一版不额外制造 MessageClient、
 * DefaultMessageClient、SendExecutor 等多层门面。</p>
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

    /**
     * 创建 MessageTemplate。
     *
     * <p>{@code options}：组件参数</p>
     * <p>{@code providerRegistry}：Provider 注册表</p>
     * <p>{@code destinationResolver}：目的地解析器</p>
     * <p>{@code envelopeEnricher}：消息丰富器</p>
     * <p>{@code wireCodec}：线级映射器</p>
     * <p>{@code contextAccessor}：当前消息访问器</p>
     */
    public MessageTemplate(
            MessageComponentOptions options,
            MessageProviderRegistry providerRegistry,
            DestinationResolver destinationResolver,
            MessageEnvelopeEnricher envelopeEnricher,
            MessageWireCodec wireCodec,
            MessageContextAccessor contextAccessor) {
        // 所有核心依赖都必须显式提供，避免隐藏的全局静态状态。
        this.options = Objects.requireNonNull(options, "options must not be null");
        // 保存 Provider 注册表。
        this.providerRegistry = Objects.requireNonNull(
                providerRegistry,
                "providerRegistry must not be null");
        // 保存目的地解析器。
        this.destinationResolver = Objects.requireNonNull(
                destinationResolver,
                "destinationResolver must not be null");
        // 保存消息丰富器。
        this.envelopeEnricher = Objects.requireNonNull(
                envelopeEnricher,
                "envelopeEnricher must not be null");
        // 保存线级映射器。
        this.wireCodec = Objects.requireNonNull(wireCodec, "wireCodec must not be null");
        // 保存当前消息上下文访问器。
        this.contextAccessor = Objects.requireNonNull(
                contextAccessor,
                "contextAccessor must not be null");
    }

    /**
     * 创建常用默认 MessageTemplate。
     *
     * <p>{@code options}：组件参数</p>
     * <p>{@code providerRegistry}：Provider 注册表</p>
     * <p>{@code routeRegistry}：路由注册表</p>
     * <p>{@code serializer}：消息序列化器</p>
     * @return 可用模板
     */
    public static MessageTemplate create(
            MessageComponentOptions options,
            MessageProviderRegistry providerRegistry,
            DestinationRouteRegistry routeRegistry,
            com.xjtu.iron.message.api.MessageSerializer serializer) {
        // 创建共享 ThreadLocal 上下文访问器。
        ThreadLocalMessageContextAccessor contextAccessor =
                new ThreadLocalMessageContextAccessor();
        // 创建默认目的地解析器。
        DefaultDestinationResolver destinationResolver =
                new DefaultDestinationResolver(
                        routeRegistry,
                        options.defaultProviderName(),
                        options.routingMode());
        // 创建默认 UUID 消息丰富器。
        MessageEnvelopeEnricher envelopeEnricher =
                new MessageEnvelopeEnricher(
                        options,
                        new UuidMessageIdGenerator(),
                        contextAccessor);
        // 创建统一线级映射器。
        MessageWireCodec wireCodec = new MessageWireCodec(serializer);
        // 返回完整模板。
        return new MessageTemplate(
                options,
                providerRegistry,
                destinationResolver,
                envelopeEnricher,
                wireCodec,
                contextAccessor);
    }

    /**
     * 同步发送普通消息。
     */
    @Override
    public SendResult send(
            MessageDestination destination,
            MessageEnvelope<?> message,
            SendOptions options) {
        // 记录整个发送调用开始时间。
        Instant startedAt = this.options.clock().instant();
        // 准备发送操作；失败时直接转换为标准结果。
        PreparedSend prepared;
        // 捕获准备阶段异常。
        try {
            // 依次完成丰富、路由、Provider 选择、序列化和线级映射。
            prepared = prepare(destination, message, options, startedAt);
        } catch (PreparationException exception) {
            // 返回明确准备失败结果。
            return preparationFailure(destination, message, startedAt, exception);
        }
        // 调用 Provider 并等待结果。
        try {
            // Provider SPI 统一使用异步发送。
            CompletionStage<ProviderSendResult> providerStage =
                    prepared.provider().send(prepared.request());
            // Provider 不允许返回 null CompletionStage。
            if (providerStage == null) {
                // null 属于 Provider 客户端实现错误。
                return failureResult(
                        prepared,
                        SendStatus.FAILED,
                        SendStage.SEND,
                        SendFailureType.CLIENT_ERROR,
                        "provider returned null completion stage");
            }
            // 在调用级确认超时内等待 Broker 或 Provider 结果。
            ProviderSendResult providerResult = providerStage.toCompletableFuture().get(
                    prepared.confirmTimeout().toMillis(),
                    TimeUnit.MILLISECONDS);
            // Provider 不允许返回 null 结果。
            if (providerResult == null) {
                // 返回标准客户端错误。
                return failureResult(
                        prepared,
                        SendStatus.FAILED,
                        SendStage.CONFIRM,
                        SendFailureType.CLIENT_ERROR,
                        "provider returned null send result");
            }
            // 将 Provider 结果映射为公共发送结果。
            return mapProviderResult(prepared, providerResult);
        } catch (InterruptedException exception) {
            // 恢复线程中断标记。
            Thread.currentThread().interrupt();
            // 中断时 Broker 可能已经接收消息，因此使用 UNKNOWN。
            return failureResult(
                    prepared,
                    SendStatus.UNKNOWN,
                    SendStage.CONFIRM,
                    SendFailureType.INTERRUPTED,
                    "thread interrupted while waiting for send confirmation");
        } catch (TimeoutException exception) {
            // 超时无法证明 Broker 未接收消息。
            return failureResult(
                    prepared,
                    SendStatus.UNKNOWN,
                    SendStage.CONFIRM,
                    SendFailureType.TIMEOUT,
                    "send confirmation timeout after " + prepared.confirmTimeout());
        } catch (ExecutionException exception) {
            // 解包 Provider Future 异常。
            return providerThrowableResult(prepared, unwrap(exception));
        } catch (RuntimeException exception) {
            // 捕获 Provider 同步抛出的客户端错误。
            return providerThrowableResult(prepared, exception);
        }
    }

    /**
     * 异步发送普通消息。
     */
    @Override
    public CompletionStage<SendResult> sendAsync(
            MessageDestination destination,
            MessageEnvelope<?> message,
            SendOptions options) {
        // 记录发送开始时间。
        Instant startedAt = this.options.clock().instant();
        // 准备发送操作。
        PreparedSend prepared;
        // 捕获准备阶段异常并返回已完成 Future。
        try {
            // 执行公共准备流程。
            prepared = prepare(destination, message, options, startedAt);
        } catch (PreparationException exception) {
            // 异步接口也使用标准 SendResult，而不是异常完成。
            return CompletableFuture.completedFuture(
                    preparationFailure(destination, message, startedAt, exception));
        }
        // 调用 Provider。
        CompletionStage<ProviderSendResult> providerStage;
        // 捕获 Provider 同步异常。
        try {
            // 发起异步发送。
            providerStage = prepared.provider().send(prepared.request());
        } catch (RuntimeException exception) {
            // 返回已完成标准失败结果。
            return CompletableFuture.completedFuture(
                    providerThrowableResult(prepared, exception));
        }
        // Provider 返回 null 属于实现错误。
        if (providerStage == null) {
            // 返回已完成客户端错误。
            return CompletableFuture.completedFuture(failureResult(
                    prepared,
                    SendStatus.FAILED,
                    SendStage.SEND,
                    SendFailureType.CLIENT_ERROR,
                    "provider returned null completion stage"));
        }
        // 创建公共结果 Future。
        CompletableFuture<SendResult> resultFuture = new CompletableFuture<>();
        // 将 Provider Future 转换为 CompletableFuture 并施加调用级超时。
        providerStage.toCompletableFuture()
                .orTimeout(
                        prepared.confirmTimeout().toMillis(),
                        TimeUnit.MILLISECONDS)
                .whenComplete((providerResult, throwable) -> {
                    // 异常完成分支。
                    if (throwable != null) {
                        // 解包 CompletionException 并转换为标准结果。
                        resultFuture.complete(
                                providerThrowableResult(prepared, unwrap(throwable)));
                        // 结束当前回调。
                        return;
                    }
                    // Provider 返回 null 结果属于实现错误。
                    if (providerResult == null) {
                        // 完成客户端错误结果。
                        resultFuture.complete(failureResult(
                                prepared,
                                SendStatus.FAILED,
                                SendStage.CONFIRM,
                                SendFailureType.CLIENT_ERROR,
                                "provider returned null send result"));
                        // 结束当前回调。
                        return;
                    }
                    // 完成映射后的标准结果。
                    resultFuture.complete(mapProviderResult(prepared, providerResult));
                });
        // 返回公共结果 Future。
        return resultFuture;
    }

    /**
     * 注册并启动普通消息消费者。
     */
    @Override
    public <T> MessageSubscription subscribe(
            ConsumerDefinition<T> definition,
            MessageHandler<T> handler) {
        // 消费者定义不能为空。
        Objects.requireNonNull(definition, "definition must not be null");
        // Handler 不能为空。
        Objects.requireNonNull(handler, "handler must not be null");
        // 解析逻辑目的地。
        ProviderDestination providerDestination =
                destinationResolver.resolve(definition.destination());
        // 获取实际 Provider。
        MessageProvider provider = providerRegistry.getRequired(
                providerDestination.providerName());
        // 校验普通消费能力。
        requireCapability(provider, MessageCapability.BASIC_CONSUME);
        // 构造 Provider 订阅请求。
        ProviderSubscriptionRequest providerRequest = new ProviderSubscriptionRequest(
                providerDestination,
                definition.consumerGroup(),
                inbound -> handleInbound(
                        definition,
                        handler,
                        providerDestination,
                        inbound));
        // 启动 Provider 订阅。
        ProviderSubscription providerSubscription = provider.subscribe(providerRequest);
        // Provider 不允许返回 null 订阅句柄。
        if (providerSubscription == null) {
            // 启动阶段直接失败，避免业务误以为已经消费。
            throw new IllegalStateException("provider returned null subscription");
        }
        // 返回 API 层关闭句柄。
        return providerSubscription::close;
    }

    /**
     * 关闭全部 Provider。
     */
    @Override
    public void close() {
        // ProviderRegistry 负责逐个释放底层资源。
        providerRegistry.close();
    }

    /**
     * 准备一次发送操作。
     */
    private PreparedSend prepare(
            MessageDestination destination,
            MessageEnvelope<?> message,
            SendOptions sendOptions,
            Instant startedAt) {
        // 基础参数校验。
        if (destination == null) {
            // 使用带阶段信息的内部异常。
            throw new PreparationException(
                    SendStage.VALIDATE,
                    SendFailureType.VALIDATION_ERROR,
                    "destination must not be null",
                    null,
                    null,
                    null);
        }
        // 消息不能为空。
        if (message == null) {
            // 返回标准校验失败。
            throw new PreparationException(
                    SendStage.VALIDATE,
                    SendFailureType.VALIDATION_ERROR,
                    "message must not be null",
                    null,
                    null,
                    null);
        }
        // null 发送选项统一使用默认选项。
        SendOptions actualOptions = sendOptions == null
                ? SendOptions.defaults()
                : sendOptions;
        // 解析实际确认超时。
        Duration confirmTimeout = actualOptions.confirmTimeout() == null
                ? options.defaultConfirmTimeout()
                : actualOptions.confirmTimeout();
        // 丰富消息信封。
        MessageEnvelope<?> enrichedMessage;
        // 捕获丰富阶段异常。
        try {
            // 补齐 ID、时间和上下文。
            enrichedMessage = envelopeEnricher.enrich(message);
        } catch (RuntimeException exception) {
            // 包装丰富失败。
            throw new PreparationException(
                    SendStage.ENRICH,
                    SendFailureType.VALIDATION_ERROR,
                    exception.getMessage(),
                    exception,
                    null,
                    null);
        }
        // 解析物理目的地。
        ProviderDestination providerDestination;
        // 捕获路由异常。
        try {
            // 将逻辑目的地解析为 Provider 和物理名称。
            providerDestination = destinationResolver.resolve(destination);
        } catch (RuntimeException exception) {
            // 包装路由失败并保留已生成 messageId。
            throw new PreparationException(
                    SendStage.RESOLVE,
                    SendFailureType.ROUTING_ERROR,
                    exception.getMessage(),
                    exception,
                    enrichedMessage,
                    null);
        }
        // 获取 Provider。
        MessageProvider provider;
        // 捕获 Provider 不存在异常。
        try {
            // 按解析结果选择 Provider。
            provider = providerRegistry.getRequired(providerDestination.providerName());
        } catch (RuntimeException exception) {
            // 标准化为 PROVIDER_NOT_FOUND。
            throw new PreparationException(
                    SendStage.RESOLVE,
                    SendFailureType.PROVIDER_NOT_FOUND,
                    exception.getMessage(),
                    exception,
                    enrichedMessage,
                    providerDestination);
        }
        // 校验普通发布能力。
        if (!provider.capabilities().contains(MessageCapability.BASIC_PUBLISH)) {
            // 不允许静默降级或尝试未知发送方式。
            throw new PreparationException(
                    SendStage.RESOLVE,
                    SendFailureType.UNSUPPORTED_CAPABILITY,
                    "provider does not support BASIC_PUBLISH: " + provider.name(),
                    null,
                    enrichedMessage,
                    providerDestination);
        }
        // 序列化并构建 Provider 请求。
        ProviderSendRequest request;
        // 捕获序列化或线级映射异常。
        try {
            // MessageWireCodec 内部先序列化，再写入完整系统消息头。
            request = wireCodec.encode(
                    destination,
                    providerDestination,
                    enrichedMessage);
        } catch (RuntimeException exception) {
            // 统一标记序列化阶段失败。
            throw new PreparationException(
                    SendStage.SERIALIZE,
                    SendFailureType.SERIALIZATION_ERROR,
                    exception.getMessage(),
                    exception,
                    enrichedMessage,
                    providerDestination);
        }
        // 返回完整准备对象。
        return new PreparedSend(
                destination,
                enrichedMessage,
                providerDestination,
                provider,
                request,
                confirmTimeout,
                startedAt);
    }

    /**
     * 处理一条 Provider 入站消息。
     */
    @SuppressWarnings("try")
    private <T> ConsumeDecision handleInbound(
            ConsumerDefinition<T> definition,
            MessageHandler<T> handler,
            ProviderDestination providerDestination,
            ProviderInboundMessage inbound) {
        // 任意异常默认转为 RETRY，防止误确认。
        try {
            // 将线级消息还原为统一信封和消费上下文。
            MessageWireCodec.DecodedInbound<T> decoded =
                    wireCodec.decode(
                            definition,
                            providerDestination,
                            inbound);
            // 打开当前消息上下文作用域。
            try (MessageContextAccessor.Scope ignored = contextAccessor.open(
                    new CurrentMessage(
                            decoded.envelope(),
                            decoded.consumeContext()))) {
                // 调用业务 Handler。
                ConsumeDecision decision = handler.handle(
                        decoded.envelope(),
                        decoded.consumeContext());
                // null 决策不能被视为成功。
                return decision == null ? ConsumeDecision.RETRY : decision;
            }
        } catch (RuntimeException exception) {
            // 一期没有死信语义，反序列化失败和业务异常都保守返回 RETRY。
            return ConsumeDecision.RETRY;
        }
    }

    /**
     * 校验 Provider 能力。
     */
    private static void requireCapability(
            MessageProvider provider,
            MessageCapability capability) {
        // Provider 和能力都已经由调用方保证非空。
        if (!provider.capabilities().contains(capability)) {
            // 启动消费者时直接失败，不允许静默失效。
            throw new IllegalStateException(
                    "provider does not support " + capability + ": " + provider.name());
        }
    }

    /**
     * 将 Provider 结果映射为 API 结果。
     */
    private SendResult mapProviderResult(
            PreparedSend prepared,
            ProviderSendResult providerResult) {
        // 成功结果使用 COMPLETE 阶段，其他结果按状态推断 SEND 或 CONFIRM。
        SendStage stage = switch (providerResult.status()) {
            // Broker 明确确认后生命周期完成。
            case CONFIRMED -> SendStage.COMPLETE;
            // UNKNOWN 通常发生在确认阶段。
            case UNKNOWN -> SendStage.CONFIRM;
            // REJECTED 和明确 FAILED 通常发生在发送或 Broker 接收阶段。
            case REJECTED, FAILED -> SendStage.SEND;
        };
        // 构造公共结果。
        return new SendResult(
                prepared.message().messageId(),
                prepared.destination(),
                prepared.providerDestination().providerName(),
                prepared.providerDestination().physicalName(),
                providerResult.status(),
                stage,
                providerResult.failureType(),
                providerResult.providerMessageId(),
                providerResult.description(),
                prepared.startedAt(),
                options.clock().instant(),
                providerResult.metadata());
    }

    /**
     * 将 Provider 异常转换为标准结果。
     */
    private SendResult providerThrowableResult(
            PreparedSend prepared,
            Throwable throwable) {
        // 超时异常表示发送结果不确定。
        if (throwable instanceof TimeoutException) {
            // 返回 UNKNOWN 而不是普通失败。
            return failureResult(
                    prepared,
                    SendStatus.UNKNOWN,
                    SendStage.CONFIRM,
                    SendFailureType.TIMEOUT,
                    "send confirmation timeout after " + prepared.confirmTimeout());
        }
        // 其他 Provider Future 异常无法保证 Broker 是否接收，采用保守 UNKNOWN。
        return failureResult(
                prepared,
                SendStatus.UNKNOWN,
                SendStage.CONFIRM,
                SendFailureType.UNKNOWN_ERROR,
                throwable == null ? "unknown provider failure" : throwable.getMessage());
    }

    /**
     * 构造准备阶段失败结果。
     */
    private SendResult preparationFailure(
            MessageDestination destination,
            MessageEnvelope<?> originalMessage,
            Instant startedAt,
            PreparationException exception) {
        // 优先使用已丰富消息 ID，其次使用业务原始 ID。
        String messageId = exception.enrichedMessage() != null
                ? exception.enrichedMessage().messageId()
                : originalMessage == null ? null : originalMessage.messageId();
        // Provider 信息只有在已经完成路由时才存在。
        String providerName = exception.providerDestination() == null
                ? destination == null ? null : destination.providerHint()
                : exception.providerDestination().providerName();
        // 物理目的地只有在完成路由时存在。
        String physicalName = exception.providerDestination() == null
                ? null
                : exception.providerDestination().physicalName();
        // 校验和路由错误属于明确拒绝，其余准备错误属于明确失败。
        SendStatus status = exception.stage() == SendStage.VALIDATE
                || exception.stage() == SendStage.RESOLVE
                ? SendStatus.REJECTED
                : SendStatus.FAILED;
        // 返回标准结果。
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
                Map.of());
    }

    /**
     * 构造已准备发送操作的失败结果。
     */
    private SendResult failureResult(
            PreparedSend prepared,
            SendStatus status,
            SendStage stage,
            SendFailureType failureType,
            String description) {
        // 返回统一公共结果。
        return new SendResult(
                prepared.message().messageId(),
                prepared.destination(),
                prepared.providerDestination().providerName(),
                prepared.providerDestination().physicalName(),
                status,
                stage,
                failureType,
                null,
                description,
                prepared.startedAt(),
                options.clock().instant(),
                Map.of());
    }

    /**
     * 解包常见异步包装异常。
     */
    private static Throwable unwrap(Throwable throwable) {
        // 持续解包 ExecutionException 和 CompletionException。
        Throwable current = throwable;
        // 只在存在 cause 时继续。
        while ((current instanceof ExecutionException
                || current instanceof CompletionException)
                && current.getCause() != null) {
            // 切换到真实 cause。
            current = current.getCause();
        }
        // 返回真实异常。
        return current;
    }

    /**
     * 表示已经完成全部发送准备的不可变快照。
     */
    private static final class PreparedSend {
        /** destination 字段。 */
        private final MessageDestination destination;

        /** message 字段。 */
        private final MessageEnvelope<?> message;

        /** providerDestination 字段。 */
        private final ProviderDestination providerDestination;

        /** provider 字段。 */
        private final MessageProvider provider;

        /** request 字段。 */
        private final ProviderSendRequest request;

        /** confirmTimeout 字段。 */
        private final Duration confirmTimeout;

        /** startedAt 字段。 */
        private final Instant startedAt;

        /**
         * 创建不可变 PreparedSend。
         */
        private PreparedSend(
            MessageDestination destination,
            MessageEnvelope<?> message,
            ProviderDestination providerDestination,
            MessageProvider provider,
            ProviderSendRequest request,
            Duration confirmTimeout,
            Instant startedAt) {
            // 保存 destination。
            this.destination = destination;
            // 保存 message。
            this.message = message;
            // 保存 providerDestination。
            this.providerDestination = providerDestination;
            // 保存 provider。
            this.provider = provider;
            // 保存 request。
            this.request = request;
            // 保存 confirmTimeout。
            this.confirmTimeout = confirmTimeout;
            // 保存 startedAt。
            this.startedAt = startedAt;
        }
        /**
         * 返回destination。
         *
         * @return destination
         */
        public MessageDestination destination() {
            // 返回不可变字段。
            return destination;
        }

        /**
         * 返回message。
         *
         * @return message
         */
        public MessageEnvelope<?> message() {
            // 返回不可变字段。
            return message;
        }

        /**
         * 返回providerDestination。
         *
         * @return providerDestination
         */
        public ProviderDestination providerDestination() {
            // 返回不可变字段。
            return providerDestination;
        }

        /**
         * 返回provider。
         *
         * @return provider
         */
        public MessageProvider provider() {
            // 返回不可变字段。
            return provider;
        }

        /**
         * 返回request。
         *
         * @return request
         */
        public ProviderSendRequest request() {
            // 返回不可变字段。
            return request;
        }

        /**
         * 返回confirmTimeout。
         *
         * @return confirmTimeout
         */
        public Duration confirmTimeout() {
            // 返回不可变字段。
            return confirmTimeout;
        }

        /**
         * 返回startedAt。
         *
         * @return startedAt
         */
        public Instant startedAt() {
            // 返回不可变字段。
            return startedAt;
        }

        /**
         * 按全部字段比较两个值对象。
         *
         * @param object 待比较对象
         * @return 字段值全部一致时返回 true
         */
        @Override
        public boolean equals(Object object) {
            // 同一对象直接相等。
            if (this == object) {
                return true;
            }
            // 类型不同或对象为空时不相等。
            if (object == null || getClass() != object.getClass()) {
                return false;
            }
            // 转换为当前类型后逐字段比较。
            PreparedSend other = (PreparedSend) object;
            return Objects.equals(destination, other.destination)
                    && Objects.equals(message, other.message)
                    && Objects.equals(providerDestination, other.providerDestination)
                    && Objects.equals(provider, other.provider)
                    && Objects.equals(request, other.request)
                    && Objects.equals(confirmTimeout, other.confirmTimeout)
                    && Objects.equals(startedAt, other.startedAt);
        }

        /**
         * 根据全部字段计算哈希值。
         *
         * @return 哈希值
         */
        @Override
        public int hashCode() {
            // 使用与 equals 相同的字段计算哈希值。
            return Objects.hash(destination, message, providerDestination, provider, request, confirmTimeout, startedAt);
        }

        /**
         * 返回便于诊断的字段摘要。
         *
         * @return 字符串摘要
         */
        @Override
        public String toString() {
            // 拼接全部字段，保持值对象可诊断。
            return "PreparedSend{" +
                    "destination=" + destination +
                    ", message=" + message +
                    ", providerDestination=" + providerDestination +
                    ", provider=" + provider +
                    ", request=" + request +
                    ", confirmTimeout=" + confirmTimeout +
                    ", startedAt=" + startedAt +
                    '}';
        }

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

        /**
         * 创建准备异常。
         */
        private PreparationException(
                SendStage stage,
                SendFailureType failureType,
                String message,
                Throwable cause,
                MessageEnvelope<?> enrichedMessage,
                ProviderDestination providerDestination) {
            // 保存诊断信息和原始 cause。
            super(message == null ? failureType.name() : message, cause);
            // 保存失败阶段。
            this.stage = stage;
            // 保存失败类型。
            this.failureType = failureType;
            // 保存已丰富消息。
            this.enrichedMessage = enrichedMessage;
            // 保存物理目的地。
            this.providerDestination = providerDestination;
        }

        /** @return 失败阶段 */
        private SendStage stage() {
            // 返回字段。
            return stage;
        }

        /** @return 失败类型 */
        private SendFailureType failureType() {
            // 返回字段。
            return failureType;
        }

        /** @return 已丰富消息 */
        private MessageEnvelope<?> enrichedMessage() {
            // 返回字段。
            return enrichedMessage;
        }

        /** @return 已解析物理目的地 */
        private ProviderDestination providerDestination() {
            // 返回字段。
            return providerDestination;
        }
    }
}
