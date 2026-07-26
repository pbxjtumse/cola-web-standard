package com.xjtu.iron.message.core;

import com.xjtu.iron.message.api.ConsumeContext;
import com.xjtu.iron.message.api.ConsumerDefinition;
import com.xjtu.iron.message.api.MessageContext;
import com.xjtu.iron.message.api.MessageEnvelope;
import com.xjtu.iron.message.api.MessageHeaders;
import com.xjtu.iron.message.api.MessageSerializer;
import com.xjtu.iron.message.spi.ProviderDestination;
import com.xjtu.iron.message.spi.ProviderInboundMessage;
import com.xjtu.iron.message.spi.ProviderSendRequest;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 负责统一消息信封与 Provider 线级消息之间的转换。
 */
public final class MessageWireMapper {

    /** 业务消息体序列化器。 */
    private final MessageSerializer serializer;

    /**
     * 创建线级映射器。
     *
     * @param serializer 消息体序列化器
     */
    public MessageWireMapper(MessageSerializer serializer) {
        // 序列化器不能为空。
        this.serializer = Objects.requireNonNull(serializer, "serializer must not be null");
    }

    /**
     * 将已丰富消息转换为 Provider 发送请求。
     *
     * @param destination 逻辑目的地
     * @param providerDestination 物理目的地
     * @param message 已丰富消息
     * @return Provider 发送请求
     */
    public ProviderSendRequest toProviderRequest(
            com.xjtu.iron.message.api.MessageDestination destination,
            ProviderDestination providerDestination,
            MessageEnvelope<?> message) {
        // 先序列化业务消息体。
        byte[] body = serializer.serialize(message.payload());
        // 构建完整线级消息头。
        Map<String, String> wireHeaders = toWireHeaders(destination, message);
        // 创建 Provider SPI 请求。
        return new ProviderSendRequest(
                providerDestination,
                message.messageId(),
                message.key(),
                wireHeaders,
                body);
    }

    /**
     * 将 Provider 原始消息还原为统一消息和消费上下文。
     *
     * @param definition 消费者定义
     * @param providerDestination 物理目的地
     * @param inbound 原始入站消息
     * @param <T> 业务消息体类型
     * @return 已解码入站对象
     */
    public <T> DecodedInbound<T> fromProviderMessage(
            ConsumerDefinition<T> definition,
            ProviderDestination providerDestination,
            ProviderInboundMessage inbound) {
        // 入站消息头不能为空 Map；ProviderInboundMessage 已完成标准化。
        Map<String, String> headers = inbound.headers();
        // 读取必填消息 ID。
        String messageId = requireHeader(headers, MessageHeaders.MESSAGE_ID);
        // 读取必填业务消息类型。
        String messageType = requireHeader(headers, MessageHeaders.MESSAGE_TYPE);
        // 读取必填结构版本。
        String schemaVersion = requireHeader(headers, MessageHeaders.SCHEMA_VERSION);
        // 校验线级逻辑目的地与当前订阅契约一致，防止错误路由被错误反序列化。
        validateLogicalDestination(definition, headers);
        // 校验媒体类型与当前序列化器一致。
        String contentType = requireHeader(headers, MessageHeaders.CONTENT_TYPE);
        // 不一致时拒绝盲目反序列化。
        if (!serializer.contentType().equalsIgnoreCase(contentType)) {
            // 一期只有单一 Serializer，因此不支持自动按 content-type 选择。
            throw new IllegalArgumentException(
                    "unsupported message content type: " + contentType
                            + ", expected=" + serializer.contentType());
        }
        // 解析业务事件发生时间。
        Instant occurredAt = parseInstant(
                requireHeader(headers, MessageHeaders.OCCURRED_AT),
                MessageHeaders.OCCURRED_AT);
        // 解析消息创建时间。
        Instant createdAt = parseInstant(
                requireHeader(headers, MessageHeaders.CREATED_AT),
                MessageHeaders.CREATED_AT);
        // 构造稳定业务上下文，可选字段缺失时保持 null。
        MessageContext messageContext = new MessageContext(
                optionalHeader(headers, MessageHeaders.SOURCE),
                requireHeader(headers, MessageHeaders.CORRELATION_ID),
                optionalHeader(headers, MessageHeaders.CAUSATION_ID),
                optionalHeader(headers, MessageHeaders.TENANT_ID));
        // 删除组件系统头，只向业务暴露用户头和 trace 等非保留技术头。
        Map<String, String> userHeaders = extractUserHeaders(headers);
        // 反序列化业务消息体。
        T payload = serializer.deserialize(inbound.body(), definition.payloadType());
        // 构造统一入站消息信封。
        MessageEnvelope<T> envelope = MessageEnvelope.builder(messageType, payload)
                .messageId(messageId)
                .schemaVersion(schemaVersion)
                .key(inbound.key())
                .context(messageContext)
                .headers(userHeaders)
                .occurredAt(occurredAt)
                .createdAt(createdAt)
                .build();
        // 构造当前投递运行时上下文。
        ConsumeContext consumeContext = new ConsumeContext(
                providerDestination.providerName(),
                providerDestination.physicalName(),
                definition.consumerGroup(),
                inbound.providerMessageId(),
                inbound.deliveryAttempt(),
                inbound.receivedAt(),
                inbound.metadata());
        // 返回信封和运行时上下文组合。
        return new DecodedInbound<>(envelope, consumeContext);
    }

    /**
     * 返回当前序列化器。
     *
     * @return 消息序列化器
     */
    public MessageSerializer serializer() {
        // 序列化器实例不可变引用，可安全返回。
        return serializer;
    }

    /**
     * 构建完整线级消息头。
     */
    private Map<String, String> toWireHeaders(
            com.xjtu.iron.message.api.MessageDestination destination,
            MessageEnvelope<?> message) {
        // 使用有序 Map 保持日志和测试稳定。
        Map<String, String> headers = new LinkedHashMap<>(message.headers());
        // 写入消息唯一标识。
        headers.put(MessageHeaders.MESSAGE_ID, message.messageId());
        // 写入业务消息类型。
        headers.put(MessageHeaders.MESSAGE_TYPE, message.messageType());
        // 写入结构版本。
        headers.put(MessageHeaders.SCHEMA_VERSION, message.schemaVersion());
        // 写入业务事件发生时间。
        headers.put(MessageHeaders.OCCURRED_AT, message.occurredAt().toString());
        // 写入消息创建时间。
        headers.put(MessageHeaders.CREATED_AT, message.createdAt().toString());
        // 写入序列化媒体类型。
        headers.put(MessageHeaders.CONTENT_TYPE, serializer.contentType());
        // 写入逻辑目的地命名空间用于诊断和跨平台治理。
        headers.put(MessageHeaders.DESTINATION_NAMESPACE, destination.namespace());
        // 写入逻辑目的地名称。
        headers.put(MessageHeaders.DESTINATION_NAME, destination.name());
        // 写入逻辑目的地类别。
        headers.put(MessageHeaders.DESTINATION_CATEGORY, destination.category().name());
        // source 是可选字段，只在实际存在时写入。
        putIfText(headers, MessageHeaders.SOURCE, message.context().source());
        // correlationId 发送后通常存在，但仍按可选字段安全写入。
        putIfText(headers, MessageHeaders.CORRELATION_ID, message.context().correlationId());
        // 首条消息 causationId 通常为空。
        putIfText(headers, MessageHeaders.CAUSATION_ID, message.context().causationId());
        // 非多租户场景 tenantId 为空。
        putIfText(headers, MessageHeaders.TENANT_ID, message.context().tenantId());
        // 返回不可变线级消息头。
        return Collections.unmodifiableMap(new LinkedHashMap<>(headers));
    }

    /**
     * 提取用户消息头。
     */
    private static Map<String, String> extractUserHeaders(Map<String, String> wireHeaders) {
        // 使用有序 Map 保留 Provider 返回顺序。
        Map<String, String> userHeaders = new LinkedHashMap<>();
        // 逐个筛选非系统头。
        wireHeaders.forEach((name, value) -> {
            // x-iron-message-* 系统头不向业务 headers 重复暴露。
            if (!MessageHeaders.isReserved(name)) {
                // traceparent、tracestate、baggage 等技术头会被保留。
                userHeaders.put(name, value);
            }
        });
        // 返回不可变用户头。
        return Collections.unmodifiableMap(new LinkedHashMap<>(userHeaders));
    }

    /**
     * 校验入站消息声明的逻辑目的地与消费者定义一致。
     */
    private static void validateLogicalDestination(
            ConsumerDefinition<?> definition,
            Map<String, String> headers) {
        // 读取线级命名空间。
        String namespace = requireHeader(headers, MessageHeaders.DESTINATION_NAMESPACE);
        // 读取线级逻辑名称。
        String name = requireHeader(headers, MessageHeaders.DESTINATION_NAME);
        // 读取线级消息类别。
        String category = requireHeader(headers, MessageHeaders.DESTINATION_CATEGORY);
        // 当前消费者定义中的逻辑目的地。
        var expected = definition.destination();
        // 任一字段不一致都拒绝按当前契约继续处理。
        if (!expected.namespace().equals(namespace)
                || !expected.name().equals(name)
                || !expected.category().name().equalsIgnoreCase(category)) {
            // 输出期望值和实际值，便于定位错误路由或 Topic 复用问题。
            throw new IllegalArgumentException(
                    "logical destination mismatch; expected=" + expected.qualifiedName()
                            + ", actual=" + namespace + ":"
                            + category.toLowerCase(java.util.Locale.ROOT) + ":" + name);
        }
    }

    /**
     * 写入非空白可选消息头。
     */
    private static void putIfText(
            Map<String, String> headers,
            String name,
            String value) {
        // 只有非空白值才进入线级消息头。
        if (value != null && !value.isBlank()) {
            // 去除首尾空白后写入。
            headers.put(name, value.trim());
        }
    }

    /**
     * 读取必填消息头。
     */
    private static String requireHeader(Map<String, String> headers, String name) {
        // 读取消息头值。
        String value = headers.get(name);
        // 缺失或空白都视为非法线级消息。
        if (value == null || value.isBlank()) {
            // 一期返回 RETRY；二期会进入毒消息和死信治理。
            throw new IllegalArgumentException("required message header missing: " + name);
        }
        // 返回标准化值。
        return value.trim();
    }

    /**
     * 读取可选消息头。
     */
    private static String optionalHeader(Map<String, String> headers, String name) {
        // 获取原始值。
        String value = headers.get(name);
        // 缺失或空白统一返回 null。
        return value == null || value.isBlank() ? null : value.trim();
    }

    /**
     * 解析 ISO-8601 时间。
     */
    private static Instant parseInstant(String value, String headerName) {
        // 捕获格式异常并补充消息头名称。
        try {
            // Instant.parse 只接受明确时区的标准格式。
            return Instant.parse(value);
        } catch (RuntimeException exception) {
            // 包装为参数异常，供消费流程统一转为 RETRY。
            throw new IllegalArgumentException(
                    "invalid instant message header: " + headerName + "=" + value,
                    exception);
        }
    }

    /**
     * 表示已经完成解码的入站消息。
     *
     * @param envelope 统一消息信封
     * @param consumeContext 消费运行时上下文
     * @param <T> 业务消息体类型
     */
    public record DecodedInbound<T>(
            MessageEnvelope<T> envelope,
            ConsumeContext consumeContext) {

        /**
         * 校验解码结果。
         */
        public DecodedInbound {
            // 信封不能为空。
            envelope = Objects.requireNonNull(envelope, "envelope must not be null");
            // 消费上下文不能为空。
            consumeContext = Objects.requireNonNull(
                    consumeContext,
                    "consumeContext must not be null");
        }
    }
}
