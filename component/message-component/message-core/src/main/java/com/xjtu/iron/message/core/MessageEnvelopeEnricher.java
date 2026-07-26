package com.xjtu.iron.message.core;

import com.xjtu.iron.message.api.MessageContext;
import com.xjtu.iron.message.api.MessageEnvelope;

import java.time.Instant;
import java.util.Objects;

/**
 * 负责在发送前补齐消息 ID、时间、来源和关联关系。
 */
public final class MessageEnvelopeEnricher {

    /** 组件运行参数。 */
    private final MessageComponentOptions options;

    /** 消息 ID 生成器。 */
    private final MessageIdGenerator messageIdGenerator;

    /** 当前入站消息上下文访问器。 */
    private final MessageContextAccessor contextAccessor;

    /**
     * 创建消息丰富器。
     *
     * @param options 组件参数
     * @param messageIdGenerator ID 生成器
     * @param contextAccessor 当前消息访问器
     */
    public MessageEnvelopeEnricher(
            MessageComponentOptions options,
            MessageIdGenerator messageIdGenerator,
            MessageContextAccessor contextAccessor) {
        // 组件参数不能为空。
        this.options = Objects.requireNonNull(options, "options must not be null");
        // ID 生成器不能为空。
        this.messageIdGenerator = Objects.requireNonNull(
                messageIdGenerator,
                "messageIdGenerator must not be null");
        // 上下文访问器不能为空。
        this.contextAccessor = Objects.requireNonNull(
                contextAccessor,
                "contextAccessor must not be null");
    }

    /**
     * 补齐一次发送所需的全部稳定字段。
     *
     * @param message 原始业务消息
     * @param <T> 消息体类型
     * @return 已丰富消息
     */
    public <T> MessageEnvelope<T> enrich(MessageEnvelope<T> message) {
        // 原始消息不能为空。
        Objects.requireNonNull(message, "message must not be null");
        // 使用统一时钟获取当前时间。
        Instant now = options.clock().instant();
        // 业务已提供 messageId 时必须保持不变，便于 Outbox 重发和去重。
        String messageId = firstText(message.messageId(), messageIdGenerator.nextId());
        // createdAt 未提供时使用当前时间。
        Instant createdAt = message.createdAt() == null ? now : message.createdAt();
        // occurredAt 未提供时使用消息创建时间，而不是消费或发送确认时间。
        Instant occurredAt = message.occurredAt() == null ? createdAt : message.occurredAt();
        // schemaVersion 未提供时使用组件默认版本。
        String schemaVersion = firstText(
                message.schemaVersion(),
                options.defaultSchemaVersion());
        // 获取业务显式上下文；MessageEnvelope 已保证非 null。
        MessageContext explicitContext = message.context();
        // 获取当前入站消息，可能不存在。
        CurrentMessage currentMessage = contextAccessor.current().orElse(null);
        // 当前入站消息的稳定上下文。
        MessageContext parentContext = currentMessage == null
                ? MessageContext.empty()
                : currentMessage.envelope().context();
        // source 优先使用业务显式值，其次使用组件 applicationName。
        String source = firstText(explicitContext.source(), options.applicationName());
        // correlationId 优先使用业务显式值，其次继承父消息，首条消息默认使用自身 messageId。
        String correlationId = firstText(
                explicitContext.correlationId(),
                firstText(parentContext.correlationId(), messageId));
        // causationId 优先使用业务显式值，否则在消费处理期间自动指向直接父消息 ID。
        String causationId = firstText(
                explicitContext.causationId(),
                currentMessage == null ? null : currentMessage.envelope().messageId());
        // tenantId 优先使用业务显式值，否则继承父消息租户。
        String tenantId = firstText(explicitContext.tenantId(), parentContext.tenantId());
        // 构造完整稳定上下文。
        MessageContext enrichedContext = new MessageContext(
                source,
                correlationId,
                causationId,
                tenantId);
        // 复制原消息并覆盖 core 管理字段。
        return message.toBuilder()
                .messageId(messageId)
                .schemaVersion(schemaVersion)
                .context(enrichedContext)
                .occurredAt(occurredAt)
                .createdAt(createdAt)
                .build();
    }

    /**
     * 返回第一个非空白字符串。
     */
    private static String firstText(String preferred, String fallback) {
        // 首选值有效时直接返回标准化值。
        if (preferred != null && !preferred.isBlank()) {
            // 去除首尾空白。
            return preferred.trim();
        }
        // 备用值无效时返回 null。
        if (fallback == null || fallback.isBlank()) {
            // 表示两个来源都没有提供值。
            return null;
        }
        // 返回标准化备用值。
        return fallback.trim();
    }
}
