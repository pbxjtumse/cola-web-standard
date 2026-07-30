package com.xjtu.iron.retry.api.support;

import com.xjtu.iron.retry.api.BackoffStrategy;
import com.xjtu.iron.retry.api.RetryContext;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 常用退避策略工厂。
 */
public final class BackoffStrategies {

    private static final BackoffStrategy NO_BACKOFF = context -> Duration.ZERO;

    private BackoffStrategies() {
    }

    /**
     * 不等待，立即开始下一次尝试。
     */
    public static BackoffStrategy none() {
        return NO_BACKOFF;
    }

    /**
     * 每次使用相同等待时间。
     */
    public static BackoffStrategy fixed(Duration delay) {
        Duration actualDelay = requireNonNegative(delay, "delay");
        return context -> actualDelay;
    }

    /**
     * 使用指数退避，不增加随机抖动。
     */
    public static BackoffStrategy exponential(
            Duration initialDelay,
            Duration maxDelay,
            double multiplier) {
        return new ExponentialBackoffStrategy(initialDelay, maxDelay, multiplier, false);
    }

    /**
     * 使用全抖动指数退避，实际等待时间位于 0 到本次指数上限之间。
     */
    public static BackoffStrategy exponentialWithFullJitter(
            Duration initialDelay,
            Duration maxDelay,
            double multiplier) {
        return new ExponentialBackoffStrategy(initialDelay, maxDelay, multiplier, true);
    }

    private static final class ExponentialBackoffStrategy implements BackoffStrategy {

        private final long initialDelayNanos;
        private final long maxDelayNanos;
        private final double multiplier;
        private final boolean fullJitter;

        private ExponentialBackoffStrategy(
                Duration initialDelay,
                Duration maxDelay,
                double multiplier,
                boolean fullJitter) {
            Duration actualInitialDelay = requireNonNegative(initialDelay, "initialDelay");
            Duration actualMaxDelay = requireNonNegative(maxDelay, "maxDelay");
            if (actualInitialDelay.compareTo(actualMaxDelay) > 0) {
                throw new IllegalArgumentException("initialDelay must not be greater than maxDelay");
            }
            if (!Double.isFinite(multiplier) || multiplier < 1.0D) {
                throw new IllegalArgumentException("multiplier must be finite and greater than or equal to 1.0");
            }
            this.initialDelayNanos = toNanosExact(actualInitialDelay, "initialDelay");
            this.maxDelayNanos = toNanosExact(actualMaxDelay, "maxDelay");
            this.multiplier = multiplier;
            this.fullJitter = fullJitter;
        }

        @Override
        public Duration nextDelay(RetryContext context) {
            Objects.requireNonNull(context, "context must not be null");
            int completedAttempt = context.getAttempt();
            double calculated = initialDelayNanos * Math.pow(multiplier, Math.max(0, completedAttempt - 1));
            long boundedDelay = calculated >= maxDelayNanos
                    ? maxDelayNanos
                    : Math.max(0L, (long) calculated);

            if (!fullJitter || boundedDelay == 0L) {
                return Duration.ofNanos(boundedDelay);
            }

            long randomizedDelay = boundedDelay == Long.MAX_VALUE
                    ? ThreadLocalRandom.current().nextLong(Long.MAX_VALUE)
                    : ThreadLocalRandom.current().nextLong(boundedDelay + 1L);
            return Duration.ofNanos(randomizedDelay);
        }
    }

    private static Duration requireNonNegative(Duration value, String name) {
        Objects.requireNonNull(value, name + " must not be null");
        if (value.isNegative()) {
            throw new IllegalArgumentException(name + " must not be negative");
        }
        return value;
    }

    private static long toNanosExact(Duration duration, String name) {
        try {
            return duration.toNanos();
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException(name + " is too large", exception);
        }
    }
}
