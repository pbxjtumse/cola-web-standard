package com.xjtu.iron.idempotent.api.policy;

import com.xjtu.iron.idempotent.api.recovery.IdempotencyRecoveryMode;
import com.xjtu.iron.idempotent.api.recovery.IdempotencyRecoveryPolicy;
import com.xjtu.iron.idempotent.api.request.IdempotencyRequest;

import java.time.Duration;

/**
 * 一类幂等业务的稳定策略。
 *
 * <p>V1.3 把“单次请求是谁”和“这类业务平时怎么执行”拆开：
 * {@link IdempotencyRequest} 只承载 key/hash/routeKey/policyName，
 * 本类承载生命周期、恢复、Repository、窗口和锁策略。</p>
 *
 * <p>ResultPolicy 是带泛型的调用级能力，不放进这里；事务是否真正可参与，
 * 由 Repository capabilities + transaction integration 在运行时共同决定。</p>
 */
public final class IdempotencyPolicy {

    public static final String DEFAULT_NAMESPACE = "default";
    public static final Duration DEFAULT_PROCESSING_TIMEOUT = Duration.ofSeconds(30);
    public static final Duration DEFAULT_WINDOW = Duration.ofMinutes(10);
    public static final Duration DEFAULT_RETENTION = Duration.ZERO;

    private final String name;
    private final IdempotencyMode mode;
    private final String namespace;
    private final String repositoryName;
    private final Duration processingTimeout;
    private final Duration idempotencyWindow;
    private final IdempotencyWindowPolicy windowPolicy;
    private final Duration recordRetentionTtl;
    private final IdempotencyRecoveryPolicy recoveryPolicy;
    private final IdempotencyLockOptions lockOptions;

    private IdempotencyPolicy(Builder builder) {
        this.name = normalize(builder.name);
        this.mode = (builder.mode == null ? IdempotencyMode.DURABLE : builder.mode).canonical();
        this.namespace = normalize(builder.namespace) == null ? DEFAULT_NAMESPACE : builder.namespace.trim();
        this.repositoryName = normalize(builder.repositoryName);
        this.processingTimeout = builder.processingTimeout == null
                ? DEFAULT_PROCESSING_TIMEOUT : builder.processingTimeout;
        this.idempotencyWindow = builder.idempotencyWindow != null
                ? builder.idempotencyWindow
                : (mode.isWindowed() ? DEFAULT_WINDOW : null);
        this.windowPolicy = builder.windowPolicy == null
                ? IdempotencyWindowPolicy.FIXED_FROM_FIRST_ACQUIRE : builder.windowPolicy;
        this.recordRetentionTtl = builder.recordRetentionTtl == null
                ? DEFAULT_RETENTION : builder.recordRetentionTtl;
        this.recoveryPolicy = builder.recoveryPolicy != null
                ? builder.recoveryPolicy
                : (mode == IdempotencyMode.DURABLE
                    ? IdempotencyRecoveryPolicy.externalTask()
                    : IdempotencyRecoveryPolicy.none());
        this.lockOptions = builder.lockOptions == null
                ? IdempotencyLockOptions.disabled() : builder.lockOptions;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static IdempotencyPolicy durable() {
        return builder().mode(IdempotencyMode.DURABLE).build();
    }

    public static IdempotencyPolicy windowed() {
        return builder().mode(IdempotencyMode.WINDOWED).build();
    }

    /**
     * 兼容 V1.2 Options。
     */
    @SuppressWarnings("deprecation")
    public static IdempotencyPolicy fromOptions(IdempotencyOptions options) {
        if (options == null) {
            return null;
        }
        return builder()
                .mode(options.getMode())
                .namespace(options.getNamespace())
                .repositoryName(options.getRepositoryName())
                .processingTimeout(options.getProcessingTimeout())
                .idempotencyWindow(options.getIdempotencyWindow())
                .windowPolicy(options.getWindowPolicy())
                .recordRetentionTtl(options.getRecordRetentionTtl())
                .recoveryPolicy(IdempotencyRecoveryPolicy.builder()
                        .mode(options.getRecoveryMode())
                        .recoverProcessingTimeout(options.getRecoveryMode()
                                == IdempotencyRecoveryMode.EXTERNAL_TASK)
                        .recoverRetryableFailure(
                                options.getRecoveryMode() == IdempotencyRecoveryMode.EXTERNAL_TASK
                                        && options.isRecoverFailed())
                        .build())
                .lockOptions(options.getLockOptions())
                .build();
    }

    public void validate() {
        if (namespace == null || namespace.isBlank()) {
            throw new IllegalArgumentException("namespace must not be blank");
        }
        if (processingTimeout == null
                || processingTimeout.isZero()
                || processingTimeout.isNegative()) {
            throw new IllegalArgumentException("processingTimeout must be positive");
        }
        if (recordRetentionTtl == null || recordRetentionTtl.isNegative()) {
            throw new IllegalArgumentException("recordRetentionTtl must not be negative");
        }
        if (mode.isWindowed()) {
            if (idempotencyWindow == null
                    || idempotencyWindow.isZero()
                    || idempotencyWindow.isNegative()) {
                throw new IllegalArgumentException("WINDOWED requires positive idempotencyWindow");
            }
            if (idempotencyWindow.compareTo(processingTimeout) <= 0) {
                throw new IllegalArgumentException(
                        "idempotencyWindow must be greater than processingTimeout");
            }
        }
        recoveryPolicy.validate();
        lockOptions.validate();
    }

    public String getName() { return name; }
    public IdempotencyMode getMode() { return mode; }
    public String getNamespace() { return namespace; }
    public String getRepositoryName() { return repositoryName; }
    public Duration getProcessingTimeout() { return processingTimeout; }
    public Duration getIdempotencyWindow() { return idempotencyWindow; }
    public IdempotencyWindowPolicy getWindowPolicy() { return windowPolicy; }
    public Duration getRecordRetentionTtl() { return recordRetentionTtl; }
    public IdempotencyRecoveryPolicy getRecoveryPolicy() { return recoveryPolicy; }
    public IdempotencyLockOptions getLockOptions() { return lockOptions; }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public static final class Builder {
        private String name;
        private IdempotencyMode mode = IdempotencyMode.DURABLE;
        private String namespace = DEFAULT_NAMESPACE;
        private String repositoryName;
        private Duration processingTimeout = DEFAULT_PROCESSING_TIMEOUT;
        private Duration idempotencyWindow;
        private IdempotencyWindowPolicy windowPolicy = IdempotencyWindowPolicy.FIXED_FROM_FIRST_ACQUIRE;
        private Duration recordRetentionTtl = DEFAULT_RETENTION;
        private IdempotencyRecoveryPolicy recoveryPolicy;
        private IdempotencyLockOptions lockOptions = IdempotencyLockOptions.disabled();

        public Builder name(String value) { this.name = value; return this; }
        public Builder mode(IdempotencyMode value) { this.mode = value; return this; }
        public Builder namespace(String value) { this.namespace = value; return this; }
        public Builder repositoryName(String value) { this.repositoryName = value; return this; }
        public Builder processingTimeout(Duration value) { this.processingTimeout = value; return this; }
        public Builder idempotencyWindow(Duration value) { this.idempotencyWindow = value; return this; }
        public Builder windowPolicy(IdempotencyWindowPolicy value) { this.windowPolicy = value; return this; }
        public Builder recordRetentionTtl(Duration value) { this.recordRetentionTtl = value; return this; }
        public Builder recoveryPolicy(IdempotencyRecoveryPolicy value) { this.recoveryPolicy = value; return this; }
        public Builder lockOptions(IdempotencyLockOptions value) { this.lockOptions = value; return this; }

        public IdempotencyPolicy build() {
            IdempotencyPolicy policy = new IdempotencyPolicy(this);
            policy.validate();
            return policy;
        }
    }
}
