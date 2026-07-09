package com.xjtu.iron.distributed.lock.api;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 退避等待配置。
 *
 * <p>该对象用于描述抢锁失败后的重试睡眠策略。默认采用指数退避 + jitter，避免大量线程以固定间隔同时访问
 * Redis，造成“惊群”或 Redis 瞬时压力。</p>
 */
public final class RetryBackoffSpec {

    /** 默认初始退避时间。 */
    public static final Duration DEFAULT_INITIAL_DELAY = Duration.ofMillis(10);

    /** 默认最大退避时间。 */
    public static final Duration DEFAULT_MAX_DELAY = Duration.ofMillis(200);

    /** 默认指数倍数。 */
    public static final double DEFAULT_MULTIPLIER = 2.0D;

    /** 默认 jitter 比例。 */
    public static final double DEFAULT_JITTER_RATIO = 0.3D;

    /** 第一次抢锁失败后的基础睡眠时间。 */
    private final Duration initialDelay;

    /** 单次退避的最大睡眠时间。 */
    private final Duration maxDelay;

    /** 指数退避倍数。 */
    private final double multiplier;

    /** jitter 比例，范围建议为 0 到 1。 */
    private final double jitterRatio;

    private RetryBackoffSpec(Builder builder) {
        this.initialDelay = builder.initialDelay;
        this.maxDelay = builder.maxDelay;
        this.multiplier = builder.multiplier;
        this.jitterRatio = builder.jitterRatio;
        validate();
    }

    /**
     * 创建默认退避配置。
     *
     * @return 默认退避配置。
     */
    public static RetryBackoffSpec defaults() {
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
     * 根据当前重试次数计算带 jitter 的睡眠时间。
     *
     * @param attempt 从 1 开始的重试次数。
     * @return 建议睡眠时间。
     */
    public Duration nextDelay(int attempt) {
        int safeAttempt = Math.max(1, attempt);
        double pow = Math.pow(multiplier, safeAttempt - 1);
        long baseMillis = Math.min(maxDelay.toMillis(), Math.round(initialDelay.toMillis() * pow));
        long jitterMillis = Math.round(baseMillis * jitterRatio);
        long actualMillis = baseMillis;
        if (jitterMillis > 0) {
            actualMillis = baseMillis + ThreadLocalRandom.current().nextLong(0, jitterMillis + 1);
        }
        return Duration.ofMillis(Math.max(1L, actualMillis));
    }

    /**
     * 校验退避配置。
     */
    public void validate() {
        if (initialDelay == null || initialDelay.isZero() || initialDelay.isNegative()) {
            throw new IllegalArgumentException("initialDelay must be positive");
        }
        if (maxDelay == null || maxDelay.isZero() || maxDelay.isNegative()) {
            throw new IllegalArgumentException("maxDelay must be positive");
        }
        if (maxDelay.compareTo(initialDelay) < 0) {
            throw new IllegalArgumentException("maxDelay must be greater than or equal to initialDelay");
        }
        if (multiplier < 1.0D) {
            throw new IllegalArgumentException("multiplier must be greater than or equal to 1");
        }
        if (jitterRatio < 0.0D || jitterRatio > 1.0D) {
            throw new IllegalArgumentException("jitterRatio must be between 0 and 1");
        }
    }

    public Duration getInitialDelay() {
        return initialDelay;
    }

    public Duration getMaxDelay() {
        return maxDelay;
    }

    public double getMultiplier() {
        return multiplier;
    }

    public double getJitterRatio() {
        return jitterRatio;
    }

    /**
     * RetryBackoffSpec 构造器。
     */
    public static final class Builder {

        private Duration initialDelay = DEFAULT_INITIAL_DELAY;
        private Duration maxDelay = DEFAULT_MAX_DELAY;
        private double multiplier = DEFAULT_MULTIPLIER;
        private double jitterRatio = DEFAULT_JITTER_RATIO;

        private Builder() {
        }

        public Builder initialDelay(Duration initialDelay) {
            this.initialDelay = initialDelay;
            return this;
        }

        public Builder maxDelay(Duration maxDelay) {
            this.maxDelay = maxDelay;
            return this;
        }

        public Builder multiplier(double multiplier) {
            this.multiplier = multiplier;
            return this;
        }

        public Builder jitterRatio(double jitterRatio) {
            this.jitterRatio = jitterRatio;
            return this;
        }

        public RetryBackoffSpec build() {
            return new RetryBackoffSpec(this);
        }
    }
}
