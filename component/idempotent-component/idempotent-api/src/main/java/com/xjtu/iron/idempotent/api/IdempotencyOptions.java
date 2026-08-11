package com.xjtu.iron.idempotent.api;

import java.time.Duration;

/**
 * 一次幂等执行的不可变策略快照。
 *
 * <p>两个时间概念必须严格区分：</p>
 * <ul>
 *     <li>{@code processingTimeout}：PROCESSING owner 的执行权租约；超时后可被新 owner 接管。</li>
 *     <li>{@code recordTtl}：SHORT_TERM 整条记录的去重窗口；TTL 到期后同 key 可重新视为新请求。</li>
 * </ul>
 */
public final class IdempotencyOptions {

    public static final String DEFAULT_NAMESPACE = "default";
    public static final Duration DEFAULT_PROCESSING_TIMEOUT = Duration.ofSeconds(30);
    public static final Duration DEFAULT_SHORT_TERM_RECORD_TTL = Duration.ofMinutes(10);

    private final IdempotencyMode mode;
    private final String namespace;
    private final String repositoryName;
    private final Duration processingTimeout;
    private final Duration recordTtl;
    private final boolean retryOnProcessingTimeout;
    private final boolean retryFailed;
    private final boolean storeResult;
    private final IdempotencyLockOptions lockOptions;

    private IdempotencyOptions(Builder builder) {
        this.mode = builder.mode == null ? IdempotencyMode.DURABLE : builder.mode;
        this.namespace = builder.namespace == null ? DEFAULT_NAMESPACE : builder.namespace;
        this.repositoryName = builder.repositoryName;
        this.processingTimeout = builder.processingTimeout == null
                ? DEFAULT_PROCESSING_TIMEOUT : builder.processingTimeout;
        this.recordTtl = builder.recordTtl != null
                ? builder.recordTtl
                : (mode == IdempotencyMode.SHORT_TERM ? DEFAULT_SHORT_TERM_RECORD_TTL : null);
        this.retryOnProcessingTimeout = builder.retryOnProcessingTimeout;
        this.retryFailed = builder.retryFailed;
        this.storeResult = builder.storeResult;
        this.lockOptions = builder.lockOptions == null
                ? IdempotencyLockOptions.disabled() : builder.lockOptions;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static IdempotencyOptions durable() {
        return builder().mode(IdempotencyMode.DURABLE).build();
    }

    public static IdempotencyOptions shortTerm() {
        return builder().mode(IdempotencyMode.SHORT_TERM).build();
    }

    public void validate() {
        if (namespace == null || namespace.isBlank()) {
            throw new IllegalArgumentException("namespace must not be blank");
        }
        if (processingTimeout == null || processingTimeout.isZero() || processingTimeout.isNegative()) {
            throw new IllegalArgumentException("processingTimeout must be positive");
        }
        if (mode == IdempotencyMode.SHORT_TERM) {
            if (recordTtl == null || recordTtl.isZero() || recordTtl.isNegative()) {
                throw new IllegalArgumentException("SHORT_TERM requires positive recordTtl");
            }
            if (recordTtl.compareTo(processingTimeout) <= 0) {
                throw new IllegalArgumentException("recordTtl must be greater than processingTimeout");
            }
        }
        lockOptions.validate();
    }

    public IdempotencyMode getMode() {
        return mode;
    }

    public String getNamespace() {
        return namespace;
    }

    /**
     * 为空时由 Registry 按 mode 选择默认 Repository。
     */
    public String getRepositoryName() {
        return repositoryName;
    }

    public Duration getProcessingTimeout() {
        return processingTimeout;
    }

    public Duration getRecordTtl() {
        return recordTtl;
    }

    public boolean isRetryOnProcessingTimeout() {
        return retryOnProcessingTimeout;
    }

    public boolean isRetryFailed() {
        return retryFailed;
    }

    /**
     * 是否持久化第一次 SUCCESS 的结果快照，便于重复请求直接 replay。
     * 默认关闭，避免大对象、敏感信息和序列化成本无意进入幂等存储。
     */
    public boolean isStoreResult() {
        return storeResult;
    }

    public IdempotencyLockOptions getLockOptions() {
        return lockOptions;
    }

    public static final class Builder {
        private IdempotencyMode mode = IdempotencyMode.DURABLE;
        private String namespace = DEFAULT_NAMESPACE;
        private String repositoryName;
        private Duration processingTimeout = DEFAULT_PROCESSING_TIMEOUT;
        private Duration recordTtl;
        private boolean retryOnProcessingTimeout = true;
        private boolean retryFailed = true;
        private boolean storeResult;
        private IdempotencyLockOptions lockOptions = IdempotencyLockOptions.disabled();

        public Builder mode(IdempotencyMode value) {
            this.mode = value;
            return this;
        }

        public Builder namespace(String value) {
            this.namespace = value;
            return this;
        }

        public Builder repositoryName(String value) {
            this.repositoryName = value;
            return this;
        }

        public Builder processingTimeout(Duration value) {
            this.processingTimeout = value;
            return this;
        }

        public Builder recordTtl(Duration value) {
            this.recordTtl = value;
            return this;
        }

        public Builder retryOnProcessingTimeout(boolean value) {
            this.retryOnProcessingTimeout = value;
            return this;
        }

        public Builder retryFailed(boolean value) {
            this.retryFailed = value;
            return this;
        }

        public Builder storeResult(boolean value) {
            this.storeResult = value;
            return this;
        }

        public Builder lockOptions(IdempotencyLockOptions value) {
            this.lockOptions = value;
            return this;
        }

        public IdempotencyOptions build() {
            return new IdempotencyOptions(this);
        }
    }
}
