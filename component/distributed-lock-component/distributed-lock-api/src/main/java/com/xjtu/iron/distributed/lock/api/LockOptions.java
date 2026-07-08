package com.xjtu.iron.distributed.lock.api;

import java.time.Duration;
import java.util.Objects;

/**
 * 分布式锁选项。
 *
 * <p>该对象用于描述一次加锁请求的行为，包括锁命名空间、等待时间、租约时间、等待策略、自动续期、
 * fencing token 等能力。</p>
 *
 * <p>设计原则：</p>
 * <ul>
 *     <li>分布式锁不鼓励无限等待，必须显式表达 waitTime。</li>
 *     <li>分布式锁必须有 leaseTime，避免客户端崩溃后锁永久不释放。</li>
 *     <li>autoRenew 默认关闭，开启时必须设置合理的 maxRenewTime。</li>
 *     <li>核心业务写入应启用 fencingRequired，并在 DB 层做条件更新。</li>
 * </ul>
 */
public final class LockOptions {

    /**
     * 默认命名空间。
     */
    public static final String DEFAULT_NAMESPACE = "default";

    /**
     * 默认租约时间。
     */
    public static final Duration DEFAULT_LEASE_TIME = Duration.ofSeconds(30);

    /**
     * 默认续期间隔。
     */
    public static final Duration DEFAULT_RENEW_INTERVAL = Duration.ofSeconds(10);

    /**
     * 默认最大自动续期时间。
     */
    public static final Duration DEFAULT_MAX_RENEW_TIME = Duration.ofMinutes(10);

    /**
     * 命名空间。
     *
     * <p>用于隔离不同系统或不同环境中的锁 key，避免 lockName 相同导致冲突。例如 {@code marketing}、
     * {@code settle}、{@code default}。</p>
     */
    private final String namespace;

    /**
     * 获取锁的最大等待时间。
     *
     * <p>{@link Duration#ZERO} 表示不等待，尝试一次后立即返回。不要使用无限等待，否则容易耗尽业务线程池。</p>
     */
    private final Duration waitTime;

    /**
     * 锁租约时间。
     *
     * <p>加锁成功后，底层存储会为锁设置过期时间。业务必须保证临界区尽量在该时间内完成；如果不能保证，
     * 可以开启 autoRenew，但仍要设置 maxRenewTime。</p>
     */
    private final Duration leaseTime;

    /**
     * 等待策略。
     *
     * <p>用于决定首次加锁失败后如何等待和重试。</p>
     */
    private final LockWaitStrategy waitStrategy;

    /**
     * 是否开启 watchdog 自动续期。
     *
     * <p>开启后，组件会在锁过期前定期校验 ownerToken 并刷新 TTL。该能力适合执行时间不确定的长任务，
     * 不建议普通短请求默认开启。</p>
     */
    private final boolean autoRenew;

    /**
     * watchdog 续期间隔。
     *
     * <p>通常建议为 leaseTime 的 1/3，例如 leaseTime 为 30 秒，则 renewInterval 为 10 秒。</p>
     */
    private final Duration renewInterval;

    /**
     * 最大自动续期时间。
     *
     * <p>用于防止业务线程卡死后 watchdog 无限续期，导致锁长期不释放。超过该时间后组件应停止续期，
     * 让锁自然过期。</p>
     */
    private final Duration maxRenewTime;

    /**
     * 是否强制要求 fencing token。
     *
     * <p>如果为 true，但当前 Provider 不支持 fencing token，应在加锁前快速失败。fencing token 用于业务资源写入保护，
     * 可以防止旧 owner 在锁过期后恢复执行并覆盖新 owner 结果。</p>
     */
    private final boolean fencingRequired;

    /**
     * 失锁后模板是否以 LOCK_LOST 失败。
     *
     * <p>如果业务执行过程中组件发现锁已丢失，且该字段为 true，execute 模板应返回或抛出 LOCK_LOST。
     * 但该配置不能替代业务侧 fencing token 校验。</p>
     */
    private final boolean failOnLockLost;

    /**
     * 指定 Provider 名称。
     *
     * <p>为空时使用默认 Provider。非空时可以指定 {@code redis}、{@code redisson}、{@code zookeeper}、
     * {@code etcd}、{@code jdbc} 等具体实现。</p>
     */
    private final String providerName;

    /**
     * 退避重试配置。
     *
     * <p>仅在 waitStrategy 为 {@link LockWaitStrategy#BACKOFF} 或 {@link LockWaitStrategy#PUBSUB_BACKOFF}
     * 时生效。</p>
     */
    private final RetryBackoffSpec backoffSpec;

    /**
     * 解锁失败时是否抛出异常。
     *
     * <p>默认 false。一般来说业务异常不应该被 unlock 异常覆盖，因此默认只记录事件和指标。对于测试或强感知场景，
     * 可以打开该开关。</p>
     */
    private final boolean throwOnReleaseFailure;

    private LockOptions(Builder builder) {
        this.namespace = builder.namespace;
        this.waitTime = builder.waitTime;
        this.leaseTime = builder.leaseTime;
        this.waitStrategy = builder.waitStrategy;
        this.autoRenew = builder.autoRenew;
        this.renewInterval = builder.renewInterval;
        this.maxRenewTime = builder.maxRenewTime;
        this.fencingRequired = builder.fencingRequired;
        this.failOnLockLost = builder.failOnLockLost;
        this.providerName = builder.providerName;
        this.backoffSpec = builder.backoffSpec;
        this.throwOnReleaseFailure = builder.throwOnReleaseFailure;
        validate();
    }

    /**
     * 返回默认锁选项。
     *
     * <p>默认：namespace=default、waitTime=0、leaseTime=30s、NO_WAIT、不自动续期、不要求 fencing token。</p>
     *
     * @return 默认锁选项。
     */
    public static LockOptions defaults() {
        return builder().build();
    }

    /**
     * 创建构造器。
     *
     * @return 构造器。
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 校验配置合法性。
     */
    public void validate() {
        if (namespace == null || namespace.trim().isEmpty()) {
            throw new IllegalArgumentException("namespace must not be blank");
        }
        Objects.requireNonNull(waitTime, "waitTime must not be null");
        Objects.requireNonNull(leaseTime, "leaseTime must not be null");
        Objects.requireNonNull(waitStrategy, "waitStrategy must not be null");
        Objects.requireNonNull(renewInterval, "renewInterval must not be null");
        Objects.requireNonNull(maxRenewTime, "maxRenewTime must not be null");
        Objects.requireNonNull(backoffSpec, "backoffSpec must not be null");

        if (waitTime.isNegative()) {
            throw new IllegalArgumentException("waitTime must not be negative");
        }
        if (leaseTime.isNegative() || leaseTime.isZero()) {
            throw new IllegalArgumentException("leaseTime must be positive");
        }
        if (renewInterval.isNegative() || renewInterval.isZero()) {
            throw new IllegalArgumentException("renewInterval must be positive");
        }
        if (maxRenewTime.isNegative() || maxRenewTime.isZero()) {
            throw new IllegalArgumentException("maxRenewTime must be positive");
        }
        if (autoRenew && renewInterval.compareTo(leaseTime) >= 0) {
            throw new IllegalArgumentException("renewInterval must be less than leaseTime when autoRenew is enabled");
        }
        if (autoRenew && maxRenewTime.compareTo(leaseTime) < 0) {
            throw new IllegalArgumentException("maxRenewTime must be greater than or equal to leaseTime when autoRenew is enabled");
        }
        if (waitTime.isZero() && waitStrategy != LockWaitStrategy.NO_WAIT) {
            throw new IllegalArgumentException("waitStrategy must be NO_WAIT when waitTime is zero");
        }
    }

    public String getNamespace() {
        return namespace;
    }

    public Duration getWaitTime() {
        return waitTime;
    }

    public Duration getLeaseTime() {
        return leaseTime;
    }

    public LockWaitStrategy getWaitStrategy() {
        return waitStrategy;
    }

    public boolean isAutoRenew() {
        return autoRenew;
    }

    public Duration getRenewInterval() {
        return renewInterval;
    }

    public Duration getMaxRenewTime() {
        return maxRenewTime;
    }

    public boolean isFencingRequired() {
        return fencingRequired;
    }

    public boolean isFailOnLockLost() {
        return failOnLockLost;
    }

    public String getProviderName() {
        return providerName;
    }

    public RetryBackoffSpec getBackoffSpec() {
        return backoffSpec;
    }

    public boolean isThrowOnReleaseFailure() {
        return throwOnReleaseFailure;
    }

    /**
     * LockOptions 构造器。
     */
    public static final class Builder {

        private String namespace = DEFAULT_NAMESPACE;
        private Duration waitTime = Duration.ZERO;
        private Duration leaseTime = DEFAULT_LEASE_TIME;
        private LockWaitStrategy waitStrategy = LockWaitStrategy.NO_WAIT;
        private boolean autoRenew = false;
        private Duration renewInterval = DEFAULT_RENEW_INTERVAL;
        private Duration maxRenewTime = DEFAULT_MAX_RENEW_TIME;
        private boolean fencingRequired = false;
        private boolean failOnLockLost = true;
        private String providerName;
        private RetryBackoffSpec backoffSpec = RetryBackoffSpec.defaults();
        private boolean throwOnReleaseFailure = false;

        private Builder() {
        }

        public Builder namespace(String namespace) {
            this.namespace = namespace;
            return this;
        }

        public Builder waitTime(Duration waitTime) {
            this.waitTime = waitTime;
            return this;
        }

        public Builder leaseTime(Duration leaseTime) {
            this.leaseTime = leaseTime;
            return this;
        }

        public Builder waitStrategy(LockWaitStrategy waitStrategy) {
            this.waitStrategy = waitStrategy;
            return this;
        }

        public Builder autoRenew(boolean autoRenew) {
            this.autoRenew = autoRenew;
            return this;
        }

        public Builder renewInterval(Duration renewInterval) {
            this.renewInterval = renewInterval;
            return this;
        }

        public Builder maxRenewTime(Duration maxRenewTime) {
            this.maxRenewTime = maxRenewTime;
            return this;
        }

        public Builder fencingRequired(boolean fencingRequired) {
            this.fencingRequired = fencingRequired;
            return this;
        }

        public Builder failOnLockLost(boolean failOnLockLost) {
            this.failOnLockLost = failOnLockLost;
            return this;
        }

        public Builder providerName(String providerName) {
            this.providerName = providerName;
            return this;
        }

        public Builder backoffSpec(RetryBackoffSpec backoffSpec) {
            this.backoffSpec = backoffSpec;
            return this;
        }

        public Builder throwOnReleaseFailure(boolean throwOnReleaseFailure) {
            this.throwOnReleaseFailure = throwOnReleaseFailure;
            return this;
        }

        public LockOptions build() {
            return new LockOptions(this);
        }
    }
}
