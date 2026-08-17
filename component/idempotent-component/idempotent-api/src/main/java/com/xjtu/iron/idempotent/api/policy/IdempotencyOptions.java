package com.xjtu.iron.idempotent.api.policy;

import com.xjtu.iron.idempotent.api.recovery.IdempotencyRecoveryMode;
import com.xjtu.iron.idempotent.api.result.IdempotencyResultPolicy;

import java.time.Duration;

/**
 * V1.2 兼容配置对象。
 *
 * @deprecated V1.3 起请使用 {@link IdempotencyPolicy}。
 *             “请求身份”和“执行策略”已经拆开，结果回放也由独立 ResultPolicy 决定。
 */
@Deprecated
public final class IdempotencyOptions {

    public static final String DEFAULT_NAMESPACE = IdempotencyPolicy.DEFAULT_NAMESPACE;
    public static final Duration DEFAULT_PROCESSING_TIMEOUT = IdempotencyPolicy.DEFAULT_PROCESSING_TIMEOUT;
    public static final Duration DEFAULT_SHORT_TERM_WINDOW = IdempotencyPolicy.DEFAULT_WINDOW;
    public static final Duration DEFAULT_RECORD_RETENTION_TTL = IdempotencyPolicy.DEFAULT_RETENTION;

    private final IdempotencyMode mode;
    private final String namespace;
    private final String repositoryName;
    private final Duration processingTimeout;
    private final Duration idempotencyWindow;
    private final IdempotencyWindowPolicy windowPolicy;
    private final Duration recordRetentionTtl;
    private final IdempotencyRecoveryMode recoveryMode;
    private final boolean recoverFailed;
    private final boolean storeResult;
    private final IdempotencyLockOptions lockOptions;

    private IdempotencyOptions(Builder builder) {
        this.mode = builder.mode == null ? IdempotencyMode.DURABLE : builder.mode;
        this.namespace = builder.namespace == null ? DEFAULT_NAMESPACE : builder.namespace;
        this.repositoryName = builder.repositoryName;
        this.processingTimeout = builder.processingTimeout == null
                ? DEFAULT_PROCESSING_TIMEOUT : builder.processingTimeout;
        this.idempotencyWindow = builder.idempotencyWindow != null
                ? builder.idempotencyWindow
                : (mode.isWindowed() ? DEFAULT_SHORT_TERM_WINDOW : null);
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

    public static Builder builder() { return new Builder(); }
    public static IdempotencyOptions durable() { return builder().mode(IdempotencyMode.DURABLE).build(); }
    public static IdempotencyOptions shortTerm() { return builder().mode(IdempotencyMode.WINDOWED).build(); }

    public void validate() {
        toPolicy().validate();
    }

    public IdempotencyPolicy toPolicy() {
        return IdempotencyPolicy.fromOptions(this);
    }

    public IdempotencyMode getMode() { return mode; }
    public String getNamespace() { return namespace; }
    public String getRepositoryName() { return repositoryName; }
    public Duration getProcessingTimeout() { return processingTimeout; }
    public Duration getIdempotencyWindow() { return idempotencyWindow; }
    public IdempotencyWindowPolicy getWindowPolicy() { return windowPolicy; }
    public Duration getRecordRetentionTtl() { return recordRetentionTtl; }
    public IdempotencyRecoveryMode getRecoveryMode() { return recoveryMode; }
    public boolean isRecoverFailed() { return recoverFailed; }

    /**
     * @deprecated V1.3 不再由 boolean 决定结果保存。请通过 IdempotencyResultPolicy 显式选择 NONE/SNAPSHOT/REFERENCE。
     */
    @Deprecated
    public boolean isStoreResult() { return storeResult; }

    public IdempotencyLockOptions getLockOptions() { return lockOptions; }

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
