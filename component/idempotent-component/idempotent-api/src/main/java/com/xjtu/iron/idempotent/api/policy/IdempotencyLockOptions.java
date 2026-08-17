package com.xjtu.iron.idempotent.api.policy;

import java.time.Duration;

/**
 * 幂等组件调用 {@code DistributedLockClient} 时使用的精简参数。
 *
 * <p>锁只是一个可选的并发协调优化层：它只包住 Repository 的 {@code tryAcquire} 短临界区，
 * 不会包住整个业务 callback。最终正确性仍由 Repository 的唯一键 / Lua / CAS 保证。</p>
 */
public final class IdempotencyLockOptions {

    public static final Duration DEFAULT_WAIT_TIME = Duration.ZERO;
    public static final Duration DEFAULT_LEASE_TIME = Duration.ofSeconds(5);

    /** 是否启用可选分布式锁协调层。 */
    private final boolean enabled;

    /** redis/redisson/...；为空时使用 DistributedLockClient 默认 Provider。 */
    private final String providerName;

    /** 抢不到短锁时最多等待多久；默认 0 表示立即返回/降级。 */
    private final Duration waitTime;

    /** 短临界区锁租约；这里只保护 tryAcquire/tryRecover，不应配置成长业务时长。 */
    private final Duration leaseTime;

    /** 锁失败时是否继续依赖 Repository 的原子状态机保证正确性。 */
    private final boolean fallbackToStateOnFailure;

    private IdempotencyLockOptions(Builder builder) {
        this.enabled = builder.enabled;
        this.providerName = builder.providerName;
        this.waitTime = builder.waitTime == null ? DEFAULT_WAIT_TIME : builder.waitTime;
        this.leaseTime = builder.leaseTime == null ? DEFAULT_LEASE_TIME : builder.leaseTime;
        this.fallbackToStateOnFailure = builder.fallbackToStateOnFailure;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static IdempotencyLockOptions disabled() {
        return builder().enabled(false).build();
    }

    public void validate() {
        if (waitTime.isNegative()) {
            throw new IllegalArgumentException("lock waitTime must not be negative");
        }
        if (leaseTime.isZero() || leaseTime.isNegative()) {
            throw new IllegalArgumentException("lock leaseTime must be positive");
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 为空时交给 DistributedLockClient 使用自己的 default-provider。
     */
    public String getProviderName() {
        return providerName;
    }

    public Duration getWaitTime() {
        return waitTime;
    }

    public Duration getLeaseTime() {
        return leaseTime;
    }

    /**
     * true：锁不可用/未获取时继续依赖 Repository 原子状态机；
     * false：锁失败直接返回 LOCK_NOT_ACQUIRED。
     */
    public boolean isFallbackToStateOnFailure() {
        return fallbackToStateOnFailure;
    }

    public static final class Builder {
        private boolean enabled;
        private String providerName;
        private Duration waitTime = DEFAULT_WAIT_TIME;
        private Duration leaseTime = DEFAULT_LEASE_TIME;
        private boolean fallbackToStateOnFailure = true;

        public Builder enabled(boolean value) {
            this.enabled = value;
            return this;
        }

        public Builder providerName(String value) {
            this.providerName = value;
            return this;
        }

        public Builder waitTime(Duration value) {
            this.waitTime = value;
            return this;
        }

        public Builder leaseTime(Duration value) {
            this.leaseTime = value;
            return this;
        }

        public Builder fallbackToStateOnFailure(boolean value) {
            this.fallbackToStateOnFailure = value;
            return this;
        }

        public IdempotencyLockOptions build() {
            return new IdempotencyLockOptions(this);
        }
    }
}
