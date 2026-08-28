package com.xjtu.iron.message.api.consume;

import java.time.Duration;
import java.util.Objects;

/**
 * 单个消费者的消费幂等配置。
 *
 * <p>{@code storeName} 只是存储路由提示，不是表名。真实写入单表、业务独立表、分表或分库分表，
 * 由 idempotent-component 的 storage 层根据 namespace、scene、idempotencyKey 和 shardKey 决定。</p>
 */
public final class MessageIdempotencyOptions {
    private static final Duration DEFAULT_PROCESSING_TIMEOUT = Duration.ofMinutes(5);
    private static final Duration DEFAULT_RECORD_RETENTION = Duration.ofDays(7);

    private final boolean enabled;
    private final MessageIdempotencyMode mode;
    private final String namespace;
    private final String scene;
    private final String keyResolverBean;
    private final Duration processingTimeout;
    private final Duration recordRetention;
    private final int maxAttempts;
    private final MessageIdempotencyFailurePolicy failurePolicy;
    private final String storeName;

    public MessageIdempotencyOptions(
            boolean enabled,
            MessageIdempotencyMode mode,
            String namespace,
            String scene,
            String keyResolverBean,
            Duration processingTimeout,
            Duration recordRetention,
            int maxAttempts,
            MessageIdempotencyFailurePolicy failurePolicy,
            String storeName) {
        this.enabled = enabled;
        this.mode = mode == null ? MessageIdempotencyMode.MESSAGE_ID : mode;
        this.namespace = textOrDefault(namespace, "message-consume");
        this.scene = normalize(scene);
        this.keyResolverBean = normalize(keyResolverBean);
        this.processingTimeout = processingTimeout == null ? DEFAULT_PROCESSING_TIMEOUT : processingTimeout;
        this.recordRetention = recordRetention == null ? DEFAULT_RECORD_RETENTION : recordRetention;
        this.maxAttempts = maxAttempts <= 0 ? 3 : maxAttempts;
        this.failurePolicy = failurePolicy == null ? MessageIdempotencyFailurePolicy.RETRY : failurePolicy;
        this.storeName = textOrDefault(storeName, "default");
    }

    public static MessageIdempotencyOptions disabled() {
        return new MessageIdempotencyOptions(
                false,
                MessageIdempotencyMode.NONE,
                "message-consume",
                null,
                null,
                DEFAULT_PROCESSING_TIMEOUT,
                DEFAULT_RECORD_RETENTION,
                3,
                MessageIdempotencyFailurePolicy.RETRY,
                "default");
    }

    public static MessageIdempotencyOptions messageId() {
        return new MessageIdempotencyOptions(
                true,
                MessageIdempotencyMode.MESSAGE_ID,
                "message-consume",
                null,
                null,
                DEFAULT_PROCESSING_TIMEOUT,
                DEFAULT_RECORD_RETENTION,
                3,
                MessageIdempotencyFailurePolicy.RETRY,
                "default");
    }

    public boolean enabled() {
        return enabled && mode != MessageIdempotencyMode.NONE;
    }

    public MessageIdempotencyMode mode() {
        return mode;
    }

    public String namespace() {
        return namespace;
    }

    public String scene() {
        return scene;
    }

    public String keyResolverBean() {
        return keyResolverBean;
    }

    public Duration processingTimeout() {
        return processingTimeout;
    }

    public Duration recordRetention() {
        return recordRetention;
    }

    public int maxAttempts() {
        return maxAttempts;
    }

    public MessageIdempotencyFailurePolicy failurePolicy() {
        return failurePolicy;
    }

    public String storeName() {
        return storeName;
    }

    private static String textOrDefault(String value, String defaultValue) {
        String normalized = normalize(value);
        return normalized == null ? Objects.requireNonNull(defaultValue, "defaultValue must not be null") : normalized;
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
