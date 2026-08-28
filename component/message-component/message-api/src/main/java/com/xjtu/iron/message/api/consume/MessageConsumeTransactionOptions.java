package com.xjtu.iron.message.api.consume;

import java.time.Duration;

/**
 * 单个消费者的事务模板配置。
 */
public final class MessageConsumeTransactionOptions {
    private final boolean enabled;
    private final boolean required;
    private final Duration timeout;
    private final String transactionName;

    public MessageConsumeTransactionOptions(
            boolean enabled,
            boolean required,
            Duration timeout,
            String transactionName) {
        this.enabled = enabled;
        this.required = required;
        this.timeout = timeout == null ? Duration.ofSeconds(30) : timeout;
        this.transactionName = normalize(transactionName);
    }

    public static MessageConsumeTransactionOptions disabled() {
        return new MessageConsumeTransactionOptions(false, false, Duration.ofSeconds(30), null);
    }

    public boolean enabled() {
        return enabled;
    }

    public boolean required() {
        return required;
    }

    public Duration timeout() {
        return timeout;
    }

    public String transactionName() {
        return transactionName;
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
