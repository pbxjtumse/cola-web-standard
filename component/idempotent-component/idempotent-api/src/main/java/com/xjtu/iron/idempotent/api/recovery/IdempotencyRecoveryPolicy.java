package com.xjtu.iron.idempotent.api.recovery;

import java.util.Objects;

/**
 * 幂等异常执行的恢复策略。
 *
 * <p>RecoveryPolicy 只回答“什么状态允许被外部可靠任务接管”，
 * 不负责扫描、调度、线程池、MQ 投递等任务基础设施。</p>
 *
 * <p>V1.3 把 V1.2 的 {@code recoveryMode + recoverFailed boolean} 收敛成一个明确策略，
 * 避免后续继续增加多个互相组合困难的 boolean。</p>
 */
public final class IdempotencyRecoveryPolicy {

    private final IdempotencyRecoveryMode mode;
    private final boolean recoverProcessingTimeout;
    private final boolean recoverRetryableFailure;

    private IdempotencyRecoveryPolicy(Builder builder) {
        this.mode = builder.mode == null ? IdempotencyRecoveryMode.NONE : builder.mode;
        this.recoverProcessingTimeout = builder.recoverProcessingTimeout;
        this.recoverRetryableFailure = builder.recoverRetryableFailure;
    }

    public static IdempotencyRecoveryPolicy none() {
        return builder().mode(IdempotencyRecoveryMode.NONE).build();
    }

    /**
     * DURABLE 推荐默认值：
     * 外部 Reliable Task 可以接管超时 PROCESSING，也可以恢复 retryable FAILED。
     */
    public static IdempotencyRecoveryPolicy externalTask() {
        return builder()
                .mode(IdempotencyRecoveryMode.EXTERNAL_TASK)
                .recoverProcessingTimeout(true)
                .recoverRetryableFailure(true)
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public void validate() {
        Objects.requireNonNull(mode, "recovery mode must not be null");
        if (mode == IdempotencyRecoveryMode.NONE
                && (recoverProcessingTimeout || recoverRetryableFailure)) {
            throw new IllegalArgumentException(
                    "recovery mode NONE cannot enable processing-timeout/failed recovery");
        }
    }

    public IdempotencyRecoveryMode getMode() {
        return mode;
    }

    public boolean isRecoverProcessingTimeout() {
        return recoverProcessingTimeout;
    }

    public boolean isRecoverRetryableFailure() {
        return recoverRetryableFailure;
    }

    public boolean isExternalTaskEnabled() {
        return mode == IdempotencyRecoveryMode.EXTERNAL_TASK;
    }

    public static final class Builder {
        private IdempotencyRecoveryMode mode = IdempotencyRecoveryMode.NONE;
        private boolean recoverProcessingTimeout;
        private boolean recoverRetryableFailure;

        public Builder mode(IdempotencyRecoveryMode value) {
            this.mode = value;
            return this;
        }

        public Builder recoverProcessingTimeout(boolean value) {
            this.recoverProcessingTimeout = value;
            return this;
        }

        public Builder recoverRetryableFailure(boolean value) {
            this.recoverRetryableFailure = value;
            return this;
        }

        public IdempotencyRecoveryPolicy build() {
            IdempotencyRecoveryPolicy policy = new IdempotencyRecoveryPolicy(this);
            policy.validate();
            return policy;
        }
    }
}
