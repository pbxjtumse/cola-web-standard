package com.xjtu.iron.idempotent.api;

import java.time.Duration;

/**
 * 一次幂等执行的不可变策略快照。
 *
 * <p>V1.1 明确区分四个容易混淆的时间/恢复概念：</p>
 * <ul>
 *     <li>{@code processingTimeout}：PROCESSING owner 的执行权租约；</li>
 *     <li>{@code idempotencyWindow}：SHORT_TERM 语义上认作“同一个请求”的时间窗口；</li>
 *     <li>{@code recordRetentionTtl}：窗口结束后记录额外保留多久，仅用于观测/排障，不继续阻止新执行；</li>
 *     <li>{@code recoveryMode}：是否允许外部 Reliable Task 调用 recover(...) 接管。</li>
 * </ul>
 */
public final class IdempotencyOptions {

    public static final String DEFAULT_NAMESPACE = "default";
    public static final Duration DEFAULT_PROCESSING_TIMEOUT = Duration.ofSeconds(30);
    public static final Duration DEFAULT_SHORT_TERM_WINDOW = Duration.ofMinutes(10);
    public static final Duration DEFAULT_RECORD_RETENTION_TTL = Duration.ZERO;

    /** SHORT_TERM / DURABLE。决定默认 Repository 和时间窗口语义。 */
    private final IdempotencyMode mode;

    /** 业务隔离域；与 key 一起构成逻辑唯一范围。 */
    private final String namespace;

    /** 显式 Repository 名称；为空时由 Registry 按 mode 选择默认 Provider。 */
    private final String repositoryName;

    /** PROCESSING owner 的执行权租约，不是 Redis Key TTL。 */
    private final Duration processingTimeout;

    /** SHORT_TERM 语义去重窗口；DURABLE 默认为空。 */
    private final Duration idempotencyWindow;

    /** FIXED 或 SLIDING，决定窗口是否随着访问向后推进。 */
    private final IdempotencyWindowPolicy windowPolicy;

    /** 窗口结束后记录额外保留时间，仅用于观测/排障。 */
    private final Duration recordRetentionTtl;

    /** 是否允许外部 Reliable Task 调用 recover()。 */
    private final IdempotencyRecoveryMode recoveryMode;

    /** FAILED 且 failureRetryable=true 时，recover() 是否允许重新抢占。 */
    private final boolean recoverFailed;

    /** 是否保存 SUCCESS 的 resultPayload 供后续 replay。 */
    private final boolean storeResult;

    /** 可选 DistributedLockClient 短临界区协调参数。 */
    private final IdempotencyLockOptions lockOptions;

    private IdempotencyOptions(Builder builder) {
        this.mode = builder.mode == null ? IdempotencyMode.DURABLE : builder.mode;
        this.namespace = builder.namespace == null ? DEFAULT_NAMESPACE : builder.namespace;
        this.repositoryName = builder.repositoryName;
        this.processingTimeout = builder.processingTimeout == null
                ? DEFAULT_PROCESSING_TIMEOUT : builder.processingTimeout;

        this.idempotencyWindow = builder.idempotencyWindow != null
                ? builder.idempotencyWindow
                : (mode == IdempotencyMode.SHORT_TERM ? DEFAULT_SHORT_TERM_WINDOW : null);
        this.windowPolicy = builder.windowPolicy == null
                ? IdempotencyWindowPolicy.FIXED_FROM_FIRST_ACQUIRE : builder.windowPolicy;
        this.recordRetentionTtl = builder.recordRetentionTtl == null
                ? DEFAULT_RECORD_RETENTION_TTL : builder.recordRetentionTtl;
        this.recoveryMode = builder.recoveryMode != null
                ? builder.recoveryMode
                : (mode == IdempotencyMode.DURABLE
                    ? IdempotencyRecoveryMode.EXTERNAL_TASK
                    : IdempotencyRecoveryMode.NONE);
        this.recoverFailed = builder.recoverFailed;
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

    /**
     * 在真正访问 Repository 前完成策略校验。
     *
     * <p>SHORT_TERM 要求 window > processingTimeout，是为了避免 PROCESSING 还可能有效时
     * 语义窗口却已经结束并开启新 generation。</p>
     */
    public void validate() {
        if (namespace == null || namespace.isBlank()) {
            throw new IllegalArgumentException("namespace must not be blank");
        }
        if (processingTimeout == null || processingTimeout.isZero() || processingTimeout.isNegative()) {
            throw new IllegalArgumentException("processingTimeout must be positive");
        }
        if (recordRetentionTtl == null || recordRetentionTtl.isNegative()) {
            throw new IllegalArgumentException("recordRetentionTtl must not be negative");
        }
        if (mode == IdempotencyMode.SHORT_TERM) {
            if (idempotencyWindow == null || idempotencyWindow.isZero() || idempotencyWindow.isNegative()) {
                throw new IllegalArgumentException("SHORT_TERM requires positive idempotencyWindow");
            }
            if (idempotencyWindow.compareTo(processingTimeout) <= 0) {
                throw new IllegalArgumentException(
                        "idempotencyWindow must be greater than processingTimeout");
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

    /** 为空时由 Registry 按 mode 选择默认 Repository。 */
    public String getRepositoryName() {
        return repositoryName;
    }

    public Duration getProcessingTimeout() {
        return processingTimeout;
    }

    public Duration getIdempotencyWindow() {
        return idempotencyWindow;
    }

    public IdempotencyWindowPolicy getWindowPolicy() {
        return windowPolicy;
    }

    /**
     * SHORT_TERM 语义窗口结束后，物理记录还额外保留多久。
     *
     * <p>该值不应被当作幂等窗口。窗口结束后，即使记录仍存在，新的请求也可以开启新 generation。</p>
     */
    public Duration getRecordRetentionTtl() {
        return recordRetentionTtl;
    }

    public IdempotencyRecoveryMode getRecoveryMode() {
        return recoveryMode;
    }

    public boolean isRecoverFailed() {
        return recoverFailed;
    }

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
        private Duration idempotencyWindow;
        private IdempotencyWindowPolicy windowPolicy = IdempotencyWindowPolicy.FIXED_FROM_FIRST_ACQUIRE;
        private Duration recordRetentionTtl = DEFAULT_RECORD_RETENTION_TTL;
        private IdempotencyRecoveryMode recoveryMode;
        private boolean recoverFailed = true;
        private boolean storeResult;
        private IdempotencyLockOptions lockOptions = IdempotencyLockOptions.disabled();

        public Builder mode(IdempotencyMode value) { this.mode = value; return this; }
        public Builder namespace(String value) { this.namespace = value; return this; }
        public Builder repositoryName(String value) { this.repositoryName = value; return this; }
        public Builder processingTimeout(Duration value) { this.processingTimeout = value; return this; }
        public Builder idempotencyWindow(Duration value) { this.idempotencyWindow = value; return this; }
        public Builder windowPolicy(IdempotencyWindowPolicy value) { this.windowPolicy = value; return this; }
        public Builder recordRetentionTtl(Duration value) { this.recordRetentionTtl = value; return this; }
        public Builder recoveryMode(IdempotencyRecoveryMode value) { this.recoveryMode = value; return this; }
        public Builder recoverFailed(boolean value) { this.recoverFailed = value; return this; }
        public Builder storeResult(boolean value) { this.storeResult = value; return this; }
        public Builder lockOptions(IdempotencyLockOptions value) { this.lockOptions = value; return this; }

        public IdempotencyOptions build() { return new IdempotencyOptions(this); }
    }
}
