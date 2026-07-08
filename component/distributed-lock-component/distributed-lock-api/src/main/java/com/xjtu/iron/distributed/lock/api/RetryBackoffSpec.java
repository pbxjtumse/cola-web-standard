package com.xjtu.iron.distributed.lock.api;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 加锁失败后的退避重试配置。
 *
 * <p>该对象主要服务于 {@link LockWaitStrategy#BACKOFF} 和 {@link LockWaitStrategy#PUBSUB_BACKOFF}。
 * 它描述加锁失败后下一次重试前应该等待多久，避免大量客户端以固定频率同时打底层存储造成惊群。</p>
 */
public final class RetryBackoffSpec {

    /**
     * 默认初始退避时间。
     */
    public static final Duration DEFAULT_INITIAL_DELAY = Duration.ofMillis(10);

    /**
     * 默认最大退避时间。
     */
    public static final Duration DEFAULT_MAX_DELAY = Duration.ofMillis(100);

    /**
     * 默认退避倍数。
     */
    public static final double DEFAULT_MULTIPLIER = 2.0D;

    /**
     * 默认抖动比例。
     */
    public static final double DEFAULT_JITTER_RATIO = 0.30D;

    /**
     * 初始退避时间。
     */
    private final Duration initialDelay;

    /**
     * 最大退避时间。
     */
    private final Duration maxDelay;

    /**
     * 每次失败后的退避倍数。
     */
    private final double multiplier;

    /**
     * 抖动比例。
     *
     * <p>例如基础退避时间为 100ms，jitterRatio 为 0.3，则实际等待时间会落在约 70ms 到 130ms 之间。
     * 这样可以避免所有等待线程在同一时刻醒来。</p>
     */
    private final double jitterRatio;

    /**
     * 最大尝试次数。
     *
     * <p>小于等于 0 表示不按次数限制，只受 LockOptions.waitTime 限制。</p>
     */
    private final int maxAttempts;

    private RetryBackoffSpec(Builder builder) {
        this.initialDelay = builder.initialDelay;
        this.maxDelay = builder.maxDelay;
        this.multiplier = builder.multiplier;
        this.jitterRatio = builder.jitterRatio;
        this.maxAttempts = builder.maxAttempts;
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
     * 创建构造器。
     *
     * @return 构造器。
     */
    public static Builder builder() {
        return new Builder();
    }

    private void validate() {
        Objects.requireNonNull(initialDelay, "initialDelay must not be null");
        Objects.requireNonNull(maxDelay, "maxDelay must not be null");
        if (initialDelay.isNegative()) {
            throw new IllegalArgumentException("initialDelay must not be negative");
        }
        if (maxDelay.isNegative() || maxDelay.isZero()) {
            throw new IllegalArgumentException("maxDelay must be positive");
        }
        if (initialDelay.compareTo(maxDelay) > 0) {
            throw new IllegalArgumentException("initialDelay must not be greater than maxDelay");
        }
        if (multiplier < 1.0D) {
            throw new IllegalArgumentException("multiplier must be >= 1.0");
        }
        if (jitterRatio < 0.0D || jitterRatio > 1.0D) {
            throw new IllegalArgumentException("jitterRatio must be between 0.0 and 1.0");
        }
    }

    /**
     * 计算某次重试前的等待时间。
     *
     * @param attempt 第几次失败，从 1 开始。
     * @return 带 jitter 的退避时间。
     */
    public Duration nextDelay(int attempt) {
        int safeAttempt = Math.max(1, attempt);
        double multiplierPower = Math.pow(multiplier, safeAttempt - 1);
        long baseMillis = Math.round(initialDelay.toMillis() * multiplierPower);
        long cappedMillis = Math.min(baseMillis, maxDelay.toMillis());

        if (jitterRatio <= 0.0D || cappedMillis <= 0) {
            return Duration.ofMillis(cappedMillis);
        }

        long jitter = Math.round(cappedMillis * jitterRatio);
        long min = Math.max(0L, cappedMillis - jitter);
        long max = cappedMillis + jitter;
        long actual = ThreadLocalRandom.current().nextLong(min, max + 1);
        return Duration.ofMillis(actual);
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

    public int getMaxAttempts() {
        return maxAttempts;
    }

    /**
     * RetryBackoffSpec 构造器。
     */
    public static final class Builder {

        private Duration initialDelay = DEFAULT_INITIAL_DELAY;
        private Duration maxDelay = DEFAULT_MAX_DELAY;
        private double multiplier = DEFAULT_MULTIPLIER;
        private double jitterRatio = DEFAULT_JITTER_RATIO;
        private int maxAttempts = 0;

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

        public Builder maxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
            return this;
        }

        public RetryBackoffSpec build() {
            return new RetryBackoffSpec(this);
        }
    }
}
