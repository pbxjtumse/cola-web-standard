package com.xjtu.iron.message.api.consume;

import com.xjtu.iron.message.api.model.MessageDestination;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 表示一次具体消费投递的运行时上下文。
 *
 * <p>该上下文面向业务 Handler，不派生 Kafka/Pulsar/RocketMQ 子类。Provider 特有字段进入 attributes，
 * 例如 kafka.partition、kafka.offset、pulsar.ledgerId、rocketmq.queueOffset。</p>
 */
public final class ConsumeContext {
    private final String providerName;
    private final String physicalDestination;
    private final MessageDestination logicalDestination;
    private final String consumerGroup;
    private final String providerMessageId;
    private final String messageId;
    private final String messageKey;
    private final String eventType;
    private final int deliveryAttempt;
    private final Instant receivedAt;
    private final Instant startedAt;
    private final ConsumerReliabilityMode reliabilityMode;
    private final MessageIdempotencyMode idempotencyMode;
    private final String idempotencyScene;
    private final String idempotencyKey;
    private final Map<String, String> headers;
    private final Map<String, String> attributes;

    public ConsumeContext(
            String providerName,
            String physicalDestination,
            String consumerGroup,
            String providerMessageId,
            int deliveryAttempt,
            Instant receivedAt,
            Map<String, String> attributes) {
        this(
                providerName,
                physicalDestination,
                null,
                consumerGroup,
                providerMessageId,
                null,
                null,
                null,
                deliveryAttempt,
                receivedAt,
                Instant.now(),
                ConsumerReliabilityMode.AT_LEAST_ONCE,
                MessageIdempotencyMode.NONE,
                null,
                null,
                Map.of(),
                attributes);
    }

    public ConsumeContext(
            String providerName,
            String physicalDestination,
            MessageDestination logicalDestination,
            String consumerGroup,
            String providerMessageId,
            String messageId,
            String messageKey,
            String eventType,
            int deliveryAttempt,
            Instant receivedAt,
            Instant startedAt,
            ConsumerReliabilityMode reliabilityMode,
            MessageIdempotencyMode idempotencyMode,
            String idempotencyScene,
            String idempotencyKey,
            Map<String, String> headers,
            Map<String, String> attributes) {
        this.providerName = normalize(providerName);
        this.physicalDestination = normalize(physicalDestination);
        this.logicalDestination = logicalDestination;
        this.consumerGroup = normalize(consumerGroup);
        this.providerMessageId = normalize(providerMessageId);
        this.messageId = normalize(messageId);
        this.messageKey = normalize(messageKey);
        this.eventType = normalize(eventType);
        this.deliveryAttempt = Math.max(1, deliveryAttempt);
        this.receivedAt = receivedAt == null ? Instant.now() : receivedAt;
        this.startedAt = startedAt == null ? Instant.now() : startedAt;
        this.reliabilityMode = reliabilityMode == null ? ConsumerReliabilityMode.AT_LEAST_ONCE : reliabilityMode;
        this.idempotencyMode = idempotencyMode == null ? MessageIdempotencyMode.NONE : idempotencyMode;
        this.idempotencyScene = normalize(idempotencyScene);
        this.idempotencyKey = normalize(idempotencyKey);
        this.headers = immutable(headers);
        this.attributes = immutable(attributes);
    }

    public ConsumeContext withIdempotency(String scene, String key, MessageIdempotencyMode mode) {
        return new ConsumeContext(
                providerName,
                physicalDestination,
                logicalDestination,
                consumerGroup,
                providerMessageId,
                messageId,
                messageKey,
                eventType,
                deliveryAttempt,
                receivedAt,
                startedAt,
                reliabilityMode,
                mode == null ? idempotencyMode : mode,
                scene,
                key,
                headers,
                attributes);
    }

    public String providerName() { return providerName; }
    public String physicalDestination() { return physicalDestination; }
    public MessageDestination logicalDestination() { return logicalDestination; }
    public String consumerGroup() { return consumerGroup; }
    public String providerMessageId() { return providerMessageId; }
    public String messageId() { return messageId; }
    public String messageKey() { return messageKey; }
    public String eventType() { return eventType; }
    public int deliveryAttempt() { return deliveryAttempt; }
    public Instant receivedAt() { return receivedAt; }
    public Instant startedAt() { return startedAt; }
    public ConsumerReliabilityMode reliabilityMode() { return reliabilityMode; }
    public MessageIdempotencyMode idempotencyMode() { return idempotencyMode; }
    public String idempotencyScene() { return idempotencyScene; }
    public String idempotencyKey() { return idempotencyKey; }
    public Map<String, String> headers() { return headers; }
    public Map<String, String> attributes() { return attributes; }

    /** 兼容旧代码中的 metadata 命名。 */
    public Map<String, String> metadata() { return attributes; }

    private static Map<String, String> immutable(Map<String, String> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(source));
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        ConsumeContext other = (ConsumeContext) object;
        return deliveryAttempt == other.deliveryAttempt
                && Objects.equals(providerName, other.providerName)
                && Objects.equals(physicalDestination, other.physicalDestination)
                && Objects.equals(logicalDestination, other.logicalDestination)
                && Objects.equals(consumerGroup, other.consumerGroup)
                && Objects.equals(providerMessageId, other.providerMessageId)
                && Objects.equals(messageId, other.messageId)
                && Objects.equals(messageKey, other.messageKey)
                && Objects.equals(eventType, other.eventType)
                && Objects.equals(receivedAt, other.receivedAt)
                && Objects.equals(startedAt, other.startedAt)
                && reliabilityMode == other.reliabilityMode
                && idempotencyMode == other.idempotencyMode
                && Objects.equals(idempotencyScene, other.idempotencyScene)
                && Objects.equals(idempotencyKey, other.idempotencyKey)
                && Objects.equals(headers, other.headers)
                && Objects.equals(attributes, other.attributes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                providerName,
                physicalDestination,
                logicalDestination,
                consumerGroup,
                providerMessageId,
                messageId,
                messageKey,
                eventType,
                deliveryAttempt,
                receivedAt,
                startedAt,
                reliabilityMode,
                idempotencyMode,
                idempotencyScene,
                idempotencyKey,
                headers,
                attributes);
    }
}
