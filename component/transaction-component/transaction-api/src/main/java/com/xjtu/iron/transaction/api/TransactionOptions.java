package com.xjtu.iron.transaction.api;

import java.time.Duration;
import java.util.Objects;

/**
 * 一次事务执行的不可变配置快照。
 *
 * <p>命名为 Options 而不是 Definition，避免与 Spring 的
 * {@code org.springframework.transaction.TransactionDefinition} 产生概念和 import 冲突。</p>
 */
public final class TransactionOptions {

    public static final String DEFAULT_NAME = "anonymous";

    private final String name;
    private final TransactionPropagation propagation;
    private final TransactionIsolation isolation;
    private final Duration timeout;
    private final boolean readOnly;

    private TransactionOptions(Builder builder) {
        this.name = builder.name;
        this.propagation = builder.propagation;
        this.isolation = builder.isolation;
        this.timeout = builder.timeout;
        this.readOnly = builder.readOnly;
    }

    public static TransactionOptions defaults() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public String name() {
        return name;
    }

    public TransactionPropagation propagation() {
        return propagation;
    }

    public TransactionIsolation isolation() {
        return isolation;
    }

    /**
     * @return 事务超时；null 表示使用底层 Provider/事务管理器默认值。
     */
    public Duration timeout() {
        return timeout;
    }

    public boolean readOnly() {
        return readOnly;
    }

    public static final class Builder {
        private String name = DEFAULT_NAME;
        private TransactionPropagation propagation = TransactionPropagation.REQUIRED;
        private TransactionIsolation isolation = TransactionIsolation.DEFAULT;
        private Duration timeout;
        private boolean readOnly;

        private Builder() {
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder propagation(TransactionPropagation propagation) {
            this.propagation = Objects.requireNonNull(propagation, "propagation");
            return this;
        }

        public Builder isolation(TransactionIsolation isolation) {
            this.isolation = Objects.requireNonNull(isolation, "isolation");
            return this;
        }

        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder readOnly(boolean readOnly) {
            this.readOnly = readOnly;
            return this;
        }

        public TransactionOptions build() {
            return new TransactionOptions(this);
        }
    }
}
