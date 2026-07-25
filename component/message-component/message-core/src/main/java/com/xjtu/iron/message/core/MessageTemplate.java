package com.xjtu.iron.message.core;

import com.xjtu.iron.message.api.ConsumeContext;
import com.xjtu.iron.message.api.ConsumeDecision;
import com.xjtu.iron.message.api.ConsumerDefinition;
import com.xjtu.iron.message.api.MessageConsumer;
import com.xjtu.iron.message.api.MessageConsumerRegistrar;
import com.xjtu.iron.message.api.MessageDestination;
import com.xjtu.iron.message.api.MessageEnvelope;
import com.xjtu.iron.message.api.MessageHeaders;
import com.xjtu.iron.message.api.MessagePublisher;
import com.xjtu.iron.message.api.MessageSerializer;
import com.xjtu.iron.message.api.SendFailureType;
import com.xjtu.iron.message.api.SendOptions;
import com.xjtu.iron.message.api.SendResult;
import com.xjtu.iron.message.api.SendStage;
import com.xjtu.iron.message.api.SendStatus;
import com.xjtu.iron.message.api.spi.MessageCapability;
import com.xjtu.iron.message.api.spi.MessageProvider;
import com.xjtu.iron.message.api.spi.ProviderInboundMessage;
import com.xjtu.iron.message.api.spi.ProviderSendRequest;
import com.xjtu.iron.message.api.spi.ProviderSendResult;
import com.xjtu.iron.message.api.spi.ProviderSubscription;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

/**
 * 编排统一消息发送与消费生命周期的核心门面。
 *
 * <p>第一版刻意把校验、增强、序列化、Provider 选择和结果转换集中在一个类中，
 * 避免尚无第二种变化来源时拆出大量只包含一个方法的小类。</p>
 */
public final class MessageTemplate
        implements MessagePublisher, MessageConsumerRegistrar, AutoCloseable {

    /** 已注册 Provider 的统一选择入口。 */
    private final MessageProviderRegistry providerRegistry;

    /** 业务对象与字节数组之间的序列化器。 */
    private final MessageSerializer serializer;

    /** 当前应用名称，用于写入消息来源。 */
    private final String applicationName;

    /** 消息结构默认版本。 */
    private final String defaultSchemaVersion;

    /** 可替换时钟，便于单元测试稳定控制时间。 */
    private final Clock clock;

    /** 可替换消息标识生成器，便于测试和业务定制。 */
    private final Supplier<String> messageIdGenerator;

    /**
     * 创建使用系统时钟和 UUID 的 MessageTemplate。
     *
     * @param providerRegistry Provider 注册表
     * @param serializer 消息序列化器
     * @param applicationName 当前应用名称
     */
    public MessageTemplate(
            MessageProviderRegistry providerRegistry,
            MessageSerializer serializer,
            String applicationName) {
        // 委托完整构造器，统一默认值来源。
        this(
                providerRegistry,
                serializer,
                applicationName,
                "1",
                Clock.systemUTC(),
                () -> UUID.randomUUID().toString());
    }

    /**
     * 创建可完整控制时间、版本和消息标识生成方式的 MessageTemplate。
     *
     * @param providerRegistry Provider 注册表
     * @param serializer 消息序列化器
     * @param applicationName 当前应用名称
     * @param defaultSchemaVersion 默认消息结构版本
     * @param clock 组件时钟
     * @param messageIdGenerator 消息标识生成器
     */
    public MessageTemplate(
            MessageProviderRegistry providerRegistry,
            MessageSerializer serializer,
            String applicationName,
            String defaultSchemaVersion,
            Clock clock,
            Supplier<String> messageIdGenerator) {
        // Provider 注册表属于必需依赖。
        this.providerRegistry = Objects.requireNonNull(providerRegistry, "providerRegistry must not be null");
        // 序列化器属于必需依赖。
        this.serializer = Objects.requireNonNull(serializer, "serializer must not be null");
        // 应用名称执行非空校验。
        this.applicationName = requireText(applicationName, "applicationName");
        // 结构版本执行非空校验。
        this.defaultSchemaVersion = requireText(defaultSchemaVersion, "defaultSchemaVersion");
        // 时钟属于必需依赖。
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        // 消息标识生成器属于必需依赖。
        this.messageIdGenerator = Objects.requireNonNull(messageIdGenerator, "messageIdGenerator must not be null");
    }

    /**
     * 同步发送消息并等待 Provider 确认。
     */
    @Override
    public SendResult send(
            MessageDestination destination,
            MessageEnvelope<?> message,
            SendOptions options) {
        // 发送准备阶段与异步发送共用同一套逻辑。
        PreparedMessage preparedMessage;
        // 捕获准备阶段异常并转换为标准结果。
        try {
            // 执行校验、增强、序列化和 Provider 选择。
            preparedMessage = prepare(destination, message, options);
        } catch (RuntimeException exception) {
            // 准备失败时仍尽可能保留业务提供的 messageId。
            return preparationFailure(message, destination, exception);
        }

        // 调用 Provider 后等待其异步结果。
        try {
            // 将 CompletionStage 转换为 CompletableFuture 以支持超时等待。
            ProviderSendResult providerResult = preparedMessage.provider()
                    .send(preparedMessage.request())
                    .toCompletableFuture()
                    .get(
                            preparedMessage.options().confirmationTimeout().toMillis(),
                            TimeUnit.MILLISECONDS);
            // 把 Provider 结果转换为公共结果。
            return mapProviderResult(preparedMessage, providerResult);
        } catch (TimeoutException exception) {
            // 等待超时无法证明 Broker 没有收到消息，因此必须返回 UNKNOWN。
            return result(
                    preparedMessage,
                    null,
                    SendStatus.UNKNOWN,
                    SendStage.CONFIRM,
                    SendFailureType.TIMEOUT,
                    "confirmation timed out");
        } catch (InterruptedException exception) {
            // 恢复线程中断标记，不能吞掉调用方取消语义。
            Thread.currentThread().interrupt();
            // 中断时同样无法确定 Broker 最终状态。
            return result(
                    preparedMessage,
                    null,
                    SendStatus.UNKNOWN,
                    SendStage.CONFIRM,
                    SendFailureType.CLIENT_ERROR,
                    "waiting thread was interrupted");
        } catch (ExecutionException exception) {
            // 提取异步执行的真实根异常。
            Throwable cause = unwrap(exception);
            // Provider 未标准化异常时由 core 兜底为客户端失败。
            return result(
                    preparedMessage,
                    null,
                    SendStatus.FAILED,
                    SendStage.SEND,
                    classifyProviderException(cause),
                    safeMessage(cause));
        } catch (RuntimeException exception) {
            // 捕获 Provider 在返回 CompletionStage 前同步抛出的异常。
            return result(
                    preparedMessage,
                    null,
                    SendStatus.FAILED,
                    SendStage.SEND,
                    classifyProviderException(exception),
                    safeMessage(exception));
        }
    }

    /**
     * 异步发送消息并异步返回标准结果。
     */
    @Override
    public CompletionStage<SendResult> sendAsync(
            MessageDestination destination,
            MessageEnvelope<?> message,
            SendOptions options) {
        // 发送准备阶段仍在调用线程同步完成，以便尽早暴露参数问题。
        PreparedMessage preparedMessage;
        // 捕获准备阶段异常并返回已完成的失败阶段。
        try {
            // 执行校验、增强、序列化和 Provider 选择。
            preparedMessage = prepare(destination, message, options);
        } catch (RuntimeException exception) {
            // 异步 API 不因可预期发送失败而额外抛出异常。
            return CompletableFuture.completedFuture(
                    preparationFailure(message, destination, exception));
        }

        // 创建由 core 控制完成方式的结果 Future。
        CompletableFuture<SendResult> resultFuture = new CompletableFuture<>();
        // 调用 Provider 可能在返回异步阶段之前同步失败。
        try {
            // 发起实际异步发送。
            preparedMessage.provider().send(preparedMessage.request())
                    .whenComplete((providerResult, throwable) -> {
                        // Provider 异步失败时执行统一异常转换。
                        if (throwable != null) {
                            // 解包 CompletionException 等包装异常。
                            Throwable cause = unwrap(throwable);
                            // 完成标准失败结果，而不是把 Provider 异常泄漏给业务。
                            resultFuture.complete(result(
                                    preparedMessage,
                                    null,
                                    SendStatus.FAILED,
                                    SendStage.SEND,
                                    classifyProviderException(cause),
                                    safeMessage(cause)));
                            // 异常分支已经完成结果，不再继续处理。
                            return;
                        }
                        // 正常分支把 Provider 结果映射为公共结果。
                        resultFuture.complete(mapProviderResult(preparedMessage, providerResult));
                    });
        } catch (RuntimeException exception) {
            // 同步调用 Provider 失败也要转为标准失败结果。
            resultFuture.complete(result(
                    preparedMessage,
                    null,
                    SendStatus.FAILED,
                    SendStage.SEND,
                    classifyProviderException(exception),
                    safeMessage(exception)));
        }
        // 返回只由上述逻辑完成的异步结果。
        return resultFuture;
    }

    /**
     * 注册一个基础消息消费者。
     */
    @Override
    public <T> MessageConsumer subscribe(ConsumerDefinition<T> definition) {
        // 消费者定义不能为空。
        Objects.requireNonNull(definition, "consumer definition must not be null");
        // 校验目的地公共字段。
        validateDestination(definition.destination());
        // 消费组必须稳定且非空。
        requireText(definition.consumerGroup(), "consumerGroup");
        // 反序列化目标类型不能为空。
        Objects.requireNonNull(definition.payloadType(), "payloadType must not be null");
        // 业务 Handler 不能为空。
        Objects.requireNonNull(definition.handler(), "handler must not be null");
        // 根据目的地选择 Provider。
        MessageProvider provider = providerRegistry.getRequired(definition.destination().providerName());
        // Provider 必须支持普通消费能力。
        requireCapability(provider, MessageCapability.BASIC_CONSUME);
        // 将业务 Handler 包装为 Provider 无关的入站监听器。
        ProviderSubscription subscription = new ProviderSubscription(
                definition.destination(),
                definition.consumerGroup(),
                inboundMessage -> handleInbound(provider, definition, inboundMessage));
        // 由 Provider 创建原生消费者并返回统一句柄。
        return provider.subscribe(subscription);
    }

    /**
     * 关闭注册表及全部 Provider。
     */
    @Override
    public void close() {
        // 统一释放三种 MQ 客户端和消费线程。
        providerRegistry.close();
    }

    /**
     * 完成发送前准备工作。
     */
    private PreparedMessage prepare(
            MessageDestination destination,
            MessageEnvelope<?> message,
            SendOptions options) {
        // 目的地不能为空且字段必须有效。
        validateDestination(destination);
        // 消息信封不能为空。
        Objects.requireNonNull(message, "message must not be null");
        // 消息类型必须稳定且非空。
        requireText(message.messageType(), "message.messageType");
        // 第一版不接受 null 消息体，避免各 Provider 对空值语义不一致。
        Objects.requireNonNull(message.payload(), "message.payload must not be null");
        // 未传 options 时使用统一默认值。
        SendOptions actualOptions = options == null ? SendOptions.defaults() : options;
        // 同步确认超时必须为正数。
        if (actualOptions.confirmationTimeout().isZero()
                || actualOptions.confirmationTimeout().isNegative()) {
            // 非正超时没有明确执行语义。
            throw new IllegalArgumentException("confirmationTimeout must be positive");
        }
        // 根据目的地选择 Provider。
        MessageProvider provider = providerRegistry.getRequired(destination.providerName());
        // Provider 必须支持普通发送能力。
        requireCapability(provider, MessageCapability.BASIC_PUBLISH);
        // 解析或生成最终消息标识。
        String actualMessageId = isBlank(message.messageId())
                ? requireText(messageIdGenerator.get(), "generated messageId")
                : message.messageId();
        // 未指定业务发生时间时以当前时间补齐。
        Instant actualOccurredAt = message.occurredAt() == null
                ? clock.instant()
                : message.occurredAt();
        // 创建受保护的系统消息头。
        Map<String, String> systemHeaders = buildSystemHeaders(
                actualMessageId,
                message.messageType(),
                actualOccurredAt);
        // 合并并覆盖系统字段。
        MessageEnvelope<?> enrichedMessage = message.enrich(
                actualMessageId,
                actualOccurredAt,
                systemHeaders);
        // 保存序列化后的消息体。
        byte[] payload;
        // 将任意序列化器异常包装为明确的序列化阶段异常。
        try {
            // 序列化业务对象。
            payload = serializer.serialize(enrichedMessage.payload());
        } catch (RuntimeException exception) {
            // 保留原始异常作为 cause，便于后续可观测性记录。
            throw new SerializationFailure("message serialization failed", exception);
        }
        // 序列化器不允许返回 null，否则 Provider 行为会不一致。
        if (payload == null) {
            // 明确指出契约违反位置。
            throw new SerializationFailure("serializer returned null payload", null);
        }
        // 创建只包含 Provider 所需数据的发送请求。
        ProviderSendRequest request = new ProviderSendRequest(
                destination,
                enrichedMessage.messageId(),
                enrichedMessage.key(),
                enrichedMessage.headers(),
                payload,
                actualOptions.providerProperties());
        // 返回后续发送和结果转换所需上下文。
        return new PreparedMessage(provider, request, actualOptions);
    }

    /**
     * 处理 Provider 交付的一条原始消息。
     */
    private <T> ConsumeDecision handleInbound(
            MessageProvider provider,
            ConsumerDefinition<T> definition,
            ProviderInboundMessage inboundMessage) {
        // Provider 不应传入 null，但 core 仍做最后防御。
        if (inboundMessage == null) {
            // 无法处理未知消息时要求 Provider 重投或记录异常。
            return ConsumeDecision.RETRY;
        }
        // 反序列化或业务处理任何异常都先转换为 RETRY。
        try {
            // 把原始字节转换为业务对象。
            T payload = serializer.deserialize(
                    inboundMessage.payload(),
                    definition.payloadType());
            // 序列化器不允许返回 null。
            if (payload == null) {
                // null 业务对象没有稳定的处理语义。
                return ConsumeDecision.RETRY;
            }
            // 创建与中间件无关的消费上下文。
            ConsumeContext context = new ConsumeContext(
                    inboundMessage.destination(),
                    provider.name(),
                    inboundMessage.nativeMessageId(),
                    inboundMessage.deliveryAttempt(),
                    clock.instant(),
                    inboundMessage.headers());
            // 调用业务 Handler。
            ConsumeDecision decision = definition.handler().handle(payload, context);
            // null 决策按失败处理，避免 Provider 误 ACK。
            return decision == null ? ConsumeDecision.RETRY : decision;
        } catch (RuntimeException exception) {
            // 第一期统一要求重投；二期会在这里接入异常分类、重试策略和死信。
            return ConsumeDecision.RETRY;
        }
    }

    /**
     * 创建标准系统消息头。
     */
    private Map<String, String> buildSystemHeaders(
            String messageId,
            String messageType,
            Instant occurredAt) {
        // 使用有序映射保持日志和测试输出稳定。
        Map<String, String> headers = new LinkedHashMap<>();
        // 写入统一消息标识。
        headers.put(MessageHeaders.MESSAGE_ID, messageId);
        // 写入稳定业务消息类型。
        headers.put(MessageHeaders.MESSAGE_TYPE, messageType);
        // 写入来源应用。
        headers.put(MessageHeaders.MESSAGE_SOURCE, applicationName);
        // 写入默认结构版本。
        headers.put(MessageHeaders.SCHEMA_VERSION, defaultSchemaVersion);
        // 写入 ISO-8601 时间字符串。
        headers.put(MessageHeaders.CREATED_AT, occurredAt.toString());
        // 返回可由 MessageEnvelope 防御复制的映射。
        return headers;
    }

    /**
     * 把 Provider 结果转换为公共发送结果。
     */
    private SendResult mapProviderResult(
            PreparedMessage preparedMessage,
            ProviderSendResult providerResult) {
        // Provider 返回 null 属于契约错误。
        if (providerResult == null) {
            // 无法判断真实发送结果，因此使用 UNKNOWN 而不是 FAILED。
            return result(
                    preparedMessage,
                    null,
                    SendStatus.UNKNOWN,
                    SendStage.CONFIRM,
                    SendFailureType.PROVIDER_ERROR,
                    "provider returned null result");
        }
        // 根据状态确定生命周期结束阶段。
        SendStage stage = providerResult.status() == SendStatus.CONFIRMED
                ? SendStage.COMPLETE
                : providerResult.status() == SendStatus.UNKNOWN
                        ? SendStage.CONFIRM
                        : SendStage.SEND;
        // 创建最终公共结果。
        return result(
                preparedMessage,
                providerResult.nativeMessageId(),
                providerResult.status(),
                stage,
                providerResult.failureType(),
                providerResult.detail());
    }

    /**
     * 创建发送准备阶段失败结果。
     */
    private SendResult preparationFailure(
            MessageEnvelope<?> message,
            MessageDestination destination,
            RuntimeException exception) {
        // 尽可能保留业务已经提供的消息标识。
        String messageId = message == null ? null : message.messageId();
        // 尽可能保留调用方指定的 Provider 名称。
        String providerName = destination == null ? null : destination.providerName();
        // 序列化失败与普通校验失败必须区分。
        SendFailureType failureType = exception instanceof SerializationFailure
                ? SendFailureType.SERIALIZATION_ERROR
                : SendFailureType.VALIDATION_ERROR;
        // 对应设置准确生命周期阶段。
        SendStage stage = failureType == SendFailureType.SERIALIZATION_ERROR
                ? SendStage.SERIALIZE
                : SendStage.VALIDATE;
        // 返回明确失败结果。
        return new SendResult(
                messageId,
                providerName,
                null,
                SendStatus.FAILED,
                stage,
                failureType,
                safeMessage(exception),
                clock.instant());
    }

    /**
     * 创建基于 PreparedMessage 的标准结果。
     */
    private SendResult result(
            PreparedMessage preparedMessage,
            String nativeMessageId,
            SendStatus status,
            SendStage stage,
            SendFailureType failureType,
            String detail) {
        // 统一填充消息标识、Provider 和完成时间。
        return new SendResult(
                preparedMessage.request().messageId(),
                preparedMessage.provider().name(),
                nativeMessageId,
                status,
                stage,
                failureType,
                detail,
                clock.instant());
    }

    /**
     * 校验消息目的地。
     */
    private static void validateDestination(MessageDestination destination) {
        // 目的地对象不能为空。
        Objects.requireNonNull(destination, "destination must not be null");
        // Provider 名称不能为空。
        requireText(destination.providerName(), "destination.providerName");
        // 逻辑目的地不能为空。
        requireText(destination.logicalName(), "destination.logicalName");
    }

    /**
     * 校验 Provider 是否支持指定公共能力。
     */
    private static void requireCapability(
            MessageProvider provider,
            MessageCapability capability) {
        // 能力集合不能为空且必须包含目标能力。
        if (provider.capabilities() == null
                || !provider.capabilities().contains(capability)) {
            // 不允许静默降级为行为不同的实现。
            throw new IllegalArgumentException(
                    "provider " + provider.name() + " does not support capability " + capability);
        }
    }

    /**
     * 校验文本字段并返回去除首尾空格后的值。
     */
    private static String requireText(String value, String fieldName) {
        // null、空串和全空格都属于非法值。
        if (isBlank(value)) {
            // 错误中携带字段名称，便于调用方定位。
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        // 返回规范化文本。
        return value.trim();
    }

    /**
     * 判断字符串是否为空白。
     */
    private static boolean isBlank(String value) {
        // 同时覆盖 null、空串和全空格。
        return value == null || value.isBlank();
    }

    /**
     * 将 Provider 异常粗粒度转换为标准失败类型。
     */
    private static SendFailureType classifyProviderException(Throwable throwable) {
        // 第一版没有引入复杂异常分类器，先根据异常类型名做保守分类。
        String typeName = throwable == null
                ? ""
                : throwable.getClass().getSimpleName().toLowerCase();
        // 含 timeout 的异常通常意味着结果可能不确定。
        if (typeName.contains("timeout")) {
            // 返回统一超时分类。
            return SendFailureType.TIMEOUT;
        }
        // 含 auth 或 permission 的异常属于认证鉴权失败。
        if (typeName.contains("auth") || typeName.contains("permission")) {
            // 返回统一认证失败分类。
            return SendFailureType.AUTHENTICATION_ERROR;
        }
        // 含 connect、network 或 io 的异常归类为网络失败。
        if (typeName.contains("connect")
                || typeName.contains("network")
                || typeName.contains("io")) {
            // 返回统一网络失败分类。
            return SendFailureType.NETWORK_ERROR;
        }
        // 其他异常暂时归入 Provider 客户端失败。
        return SendFailureType.CLIENT_ERROR;
    }

    /**
     * 解包异步执行包装异常。
     */
    private static Throwable unwrap(Throwable throwable) {
        // 逐层移除 CompletionException 和 ExecutionException。
        Throwable actual = throwable;
        // 仅在存在 cause 时继续解包。
        while ((actual instanceof CompletionException
                || actual instanceof ExecutionException)
                && actual.getCause() != null) {
            // 切换到真实根异常。
            actual = actual.getCause();
        }
        // 返回最终异常。
        return actual;
    }

    /**
     * 获取适合诊断输出的异常消息。
     */
    private static String safeMessage(Throwable throwable) {
        // null 异常返回固定说明。
        if (throwable == null) {
            // 避免结果 detail 为 null。
            return "unknown error";
        }
        // 优先使用异常原始消息。
        String message = throwable.getMessage();
        // 没有消息时回退为异常类型名称。
        return isBlank(message) ? throwable.getClass().getName() : message;
    }

    /**
     * 保存一次发送准备完成后的不可变上下文。
     *
     * @param provider 实际 Provider
     * @param request Provider 请求
     * @param options 实际发送选项
     */
    private record PreparedMessage(
            MessageProvider provider,
            ProviderSendRequest request,
            SendOptions options) {
    }

    /**
     * 标记准备流程中的序列化失败。
     */
    private static final class SerializationFailure extends RuntimeException {

        /**
         * 创建序列化失败异常。
         *
         * @param message 错误说明
         * @param cause 原始异常
         */
        private SerializationFailure(String message, Throwable cause) {
            // 将错误信息交给 RuntimeException 保存。
            super(message, cause);
        }
    }
}
