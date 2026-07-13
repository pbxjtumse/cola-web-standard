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

    /** 默认命名空间。 */
    public static final String DEFAULT_NAMESPACE = "default";

    /** 默认锁租约时间。 */
    public static final Duration DEFAULT_LEASE_TIME = Duration.ofSeconds(30);

    /** 默认续期间隔。 */
    public static final Duration DEFAULT_RENEW_INTERVAL = Duration.ofSeconds(10);

    /** 默认最大自动续期时间。 */
    public static final Duration DEFAULT_MAX_RENEW_TIME = Duration.ofMinutes(10);

    /**
     * 命名空间。
     *
     * <p>用于隔离不同系统或不同环境中的锁，避免 lockName 相同导致冲突。例如 {@code marketing}、
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
     * <p>如果 Builder 中未显式设置该字段，build 时会根据 waitTime 自动推导：waitTime 为 0 使用 NO_WAIT，
     * waitTime 大于 0 使用 BACKOFF。</p>
     */
    private final LockWaitStrategy waitStrategy;

    /**
     * 是否开启自动续期。
     *
     * <p>自动续期用于执行时间不确定的任务。开启后，watchdog 会定期执行安全续期脚本；如果续期失败，
     * 当前 handle 会被标记为 lost。</p>
     */
    private final boolean autoRenew;

    /**
     * 自动续期间隔。
     *
     * <p>建议为 leaseTime 的 1/3，例如 leaseTime=30s，renewInterval=10s。</p>
     */
    private final Duration renewInterval;

    /**
     * 最大自动续期时间。
     *
     * <p>用于防止任务卡死后 watchdog 一直续期，导致锁长期不释放。</p>
     */
    private final Duration maxRenewTime;

    /**
     * 是否要求 fencing token。
     *
     * <p>如果为 true，则一次加锁成功必须返回 fencing token；如果当前 Provider 或指定的
     * FencingTokenProvider 不支持，应在加锁前 fail fast。</p>
     */
    private final boolean fencingRequired;

    /**
     * 失锁后模板是否返回 LOCK_LOST。
     *
     * <p>该字段用于 execute 模板：如果执行期间 watchdog 或显式检查发现失锁，模板最终可以把结果标记为
     * LOCK_LOST。即使该字段为 true，业务核心写入仍需要依赖 fencing token + DB 条件更新。</p>
     */
    private final boolean failOnLockLost;

    /**
     * 指定底层锁 Provider 名称。
     *
     * <p>为空时使用组件默认 Provider；非空时从 LockProviderRegistry 中按名称选择 Provider。
     * 例如 {@code redis}、{@code redisson}、{@code zookeeper}。</p>
     */
    private final String providerName;

    /**
     * 指定 fencing token Provider 名称。
     *
     * <p>为空时优先使用 LockProvider 原生 fencing 能力；非空时使用指定 FencingTokenProvider。
     * 这个字段是为了支持“Redis 做锁、DB sequence 做 fencing token”的场景。</p>
     */
    private final String fencingTokenProviderName;

    /**
     * 创建不等待的锁选项。
     *
     * <p>
     * 语义：
     * 抢锁时只尝试一次，成功则返回 LockHandle，失败则立即返回 NOT_ACQUIRED。
     * leaseTime 使用默认 30 秒。
     * </p>
     *
     * @return 不等待锁选项
     */
    public static LockOptions noWait() {
        return builder()
                .waitTime(Duration.ZERO)
                .waitStrategy(LockWaitStrategy.NO_WAIT)
                .build();
    }

    /**
     * 创建不等待的锁选项，并指定租约时间。
     *
     * <p>
     * 语义：
     * 抢锁时只尝试一次，成功后锁租约为指定 leaseTime。
     * </p>
     *
     * @param leaseTime 锁租约时间
     * @return 不等待锁选项
     */
    public static LockOptions noWait(Duration leaseTime) {
        return builder()
                .waitTime(Duration.ZERO)
                .waitStrategy(LockWaitStrategy.NO_WAIT)
                .leaseTime(leaseTime)
                .build();
    }

    /**
     * 创建带等待时间的锁选项。
     *
     * <p>
     * 语义：
     * 在 waitTime 内使用 BACKOFF 策略重试获取锁。
     * leaseTime 使用默认 30 秒。
     * </p>
     *
     * @param waitTime 最长等待时间
     * @return 带等待时间的锁选项
     */
    public static LockOptions waitFor(Duration waitTime) {
        return builder()
                .waitTime(waitTime)
                .waitStrategy(LockWaitStrategy.BACKOFF)
                .build();
    }

    /**
     * 创建带等待时间和租约时间的锁选项。
     *
     * <p>
     * 语义：
     * 在 waitTime 内使用 BACKOFF 策略重试获取锁。
     * 获取成功后，锁租约为 leaseTime。
     * </p>
     *
     * @param waitTime 最长等待时间
     * @param leaseTime 锁租约时间
     * @return 带等待时间和租约时间的锁选项
     */
    public static LockOptions waitFor(Duration waitTime, Duration leaseTime) {
        return builder()
                .waitTime(waitTime)
                .waitStrategy(LockWaitStrategy.BACKOFF)
                .leaseTime(leaseTime)
                .build();
    }

    /**
     * 创建自动续期的锁选项。
     *
     * <p>
     * 语义：
     * 不等待抢锁，租约使用默认 30 秒。
     * 获取锁成功后开启 watchdog 自动续期，最长自动续期时间为 maxRenewTime。
     * renewInterval 默认取 leaseTime / 3。
     * </p>
     *
     * @param maxRenewTime 最大自动续期时间
     * @return 自动续期锁选项
     */
    public static LockOptions autoRenew(Duration maxRenewTime) {
        return builder()
                .waitTime(Duration.ZERO)
                .waitStrategy(LockWaitStrategy.NO_WAIT)
                .leaseTime(DEFAULT_LEASE_TIME)
                .autoRenew(maxRenewTime)
                .build();
    }

    /**
     * 创建指定租约时间和最大续期时间的自动续期锁选项。
     *
     * <p>
     * 语义：
     * 不等待抢锁。
     * 获取成功后开启 watchdog 自动续期。
     * 每次续期会把 Redis TTL 重置为 leaseTime，而不是累加 leaseTime。
     * </p>
     *
     * @param leaseTime 锁租约时间
     * @param maxRenewTime 最大自动续期时间
     * @return 自动续期锁选项
     */
    public static LockOptions autoRenew(Duration leaseTime, Duration maxRenewTime) {
        return builder()
                .waitTime(Duration.ZERO)
                .waitStrategy(LockWaitStrategy.NO_WAIT)
                .leaseTime(leaseTime)
                .autoRenew(maxRenewTime)
                .build();
    }

    /**
     * 创建带等待时间、租约时间和自动续期的锁选项。
     *
     * <p>
     * 这是长任务比较常用的配置：
     * </p>
     *
     * <ul>
     *     <li>先最多等待 waitTime 获取锁；</li>
     *     <li>获取成功后，锁租约为 leaseTime；</li>
     *     <li>业务未结束前，watchdog 自动续期；</li>
     *     <li>最多自动续期 maxRenewTime。</li>
     * </ul>
     *
     * @param waitTime 最长等待时间
     * @param leaseTime 锁租约时间
     * @param maxRenewTime 最大自动续期时间
     * @return 带等待和自动续期的锁选项
     */
    public static LockOptions waitForAndAutoRenew(
            Duration waitTime,
            Duration leaseTime,
            Duration maxRenewTime
    ) {
        return builder()
                .waitTime(waitTime)
                .waitStrategy(LockWaitStrategy.BACKOFF)
                .leaseTime(leaseTime)
                .autoRenew(maxRenewTime)
                .build();
    }

    /**
     * 退避等待配置。
     *
     * <p>仅当 waitStrategy 为 BACKOFF 或 PUBSUB_BACKOFF 时生效。</p>
     */
    private final RetryBackoffSpec backoffSpec;

    private LockOptions(Builder builder) {
        this.namespace = normalizeNamespace(builder.namespace);
        this.waitTime = builder.waitTime == null ? Duration.ZERO : builder.waitTime;
        this.leaseTime = builder.leaseTime == null ? DEFAULT_LEASE_TIME : builder.leaseTime;
        this.waitStrategy = inferWaitStrategy(builder.waitStrategy, this.waitTime);
        this.autoRenew = builder.autoRenew;
        this.renewInterval = builder.renewInterval == null ? defaultRenewInterval(this.leaseTime) : builder.renewInterval;
        this.maxRenewTime = builder.maxRenewTime == null ? DEFAULT_MAX_RENEW_TIME : builder.maxRenewTime;
        this.fencingRequired = builder.fencingRequired;
        this.failOnLockLost = builder.failOnLockLost;
        this.providerName = normalizeNullable(builder.providerName);
        this.fencingTokenProviderName = normalizeNullable(builder.fencingTokenProviderName);
        this.backoffSpec = builder.backoffSpec == null ? RetryBackoffSpec.defaults() : builder.backoffSpec;
        validate();
    }

    /**
     * 创建默认选项。
     *
     * @return 默认锁选项。
     */
    public static LockOptions defaults() {
        return builder().build();
    }

    /**
     * 创建 Builder。
     *
     * @return Builder。
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 校验锁选项是否合法。
     */
    public void validate() {
        requireNonNegative(waitTime, "waitTime");
        requirePositive(leaseTime, "leaseTime");
        requirePositive(renewInterval, "renewInterval");
        requirePositive(maxRenewTime, "maxRenewTime");
        Objects.requireNonNull(waitStrategy, "waitStrategy must not be null");
        Objects.requireNonNull(backoffSpec, "backoffSpec must not be null");
        backoffSpec.validate();

        if (waitTime.isZero() && waitStrategy != LockWaitStrategy.NO_WAIT) {
            throw new IllegalArgumentException("waitTime is zero, waitStrategy must be NO_WAIT");
        }
        if (!waitTime.isZero() && waitStrategy == LockWaitStrategy.NO_WAIT) {
            throw new IllegalArgumentException("waitTime is greater than zero, waitStrategy must not be NO_WAIT");
        }
        if (autoRenew && maxRenewTime.compareTo(leaseTime) < 0) {
            throw new IllegalArgumentException("maxRenewTime must be greater than or equal to leaseTime when autoRenew is enabled");
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

    public String getFencingTokenProviderName() {
        return fencingTokenProviderName;
    }

    public RetryBackoffSpec getBackoffSpec() {
        return backoffSpec;
    }

    private static LockWaitStrategy inferWaitStrategy(LockWaitStrategy configured, Duration waitTime) {
        if (configured != null) {
            return configured;
        }
        return waitTime == null || waitTime.isZero() ? LockWaitStrategy.NO_WAIT : LockWaitStrategy.BACKOFF;
    }

    private static Duration defaultRenewInterval(Duration leaseTime) {
        long millis = Math.max(1L, leaseTime.toMillis() / 3L);
        return Duration.ofMillis(millis);
    }

    private static String normalizeNamespace(String namespace) {
        String value = namespace == null || namespace.trim().isEmpty() ? DEFAULT_NAMESPACE : namespace.trim();
        return value;
    }

    private static String normalizeNullable(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private static void requirePositive(Duration duration, String fieldName) {
        if (duration == null || duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
    }

    private static void requireNonNegative(Duration duration, String fieldName) {
        if (duration == null || duration.isNegative()) {
            throw new IllegalArgumentException(fieldName + " must not be negative");
        }
    }

    /**
     * LockOptions 构造器。
     */
    public static final class Builder {

        /** 命名空间。 */
        private String namespace = DEFAULT_NAMESPACE;

        /** 最长等待时间。 */
        private Duration waitTime = Duration.ZERO;

        /** 锁租约时间。 */
        private Duration leaseTime = DEFAULT_LEASE_TIME;

        /** 等待策略。为空时根据 waitTime 自动推导。 */
        private LockWaitStrategy waitStrategy;

        /** 是否自动续期。 */
        private boolean autoRenew;

        /** 自动续期间隔。 */
        private Duration renewInterval;

        /** 最大自动续期时间。 */
        private Duration maxRenewTime = DEFAULT_MAX_RENEW_TIME;

        /** 是否必须返回 fencing token。 */
        private boolean fencingRequired;

        /** 失锁后模板是否返回 LOCK_LOST。 */
        private boolean failOnLockLost;

        /** 指定底层锁 Provider。 */
        private String providerName;

        /** 指定 fencing token Provider。 */
        private String fencingTokenProviderName;

        /** 退避等待配置。 */
        private RetryBackoffSpec backoffSpec = RetryBackoffSpec.defaults();

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

        /**
         * 开启自动续期，并设置最大自动续期时间。
         *
         * @param maxRenewTime 最大自动续期时间。
         * @return 当前 Builder。
         */
        public Builder autoRenew(Duration maxRenewTime) {
            this.autoRenew = true;
            this.maxRenewTime = maxRenewTime;
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

        public Builder fencingTokenProviderName(String fencingTokenProviderName) {
            this.fencingTokenProviderName = fencingTokenProviderName;
            return this;
        }

        public Builder backoffSpec(RetryBackoffSpec backoffSpec) {
            this.backoffSpec = backoffSpec;
            return this;
        }

        public LockOptions build() {
            return new LockOptions(this);
        }
    }
}
