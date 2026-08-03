package com.xjtu.iron.foundation.time;

import com.xjtu.iron.foundation.core.validation.Arguments;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * 截止时间值对象。
 *
 * <p>它适合重试、锁等待、并行任务超时和缓存过期等技术场景。Deadline 表达的是技术时间边界，
 * 不代表业务日切、清算日或活动周期。</p>
 */
public final class Deadline {

    private final Instant instant;

    private Deadline(Instant instant) {
        this.instant = Objects.requireNonNull(instant, "instant must not be null");
    }

    public static Deadline at(Instant instant) {
        return new Deadline(instant);
    }

    public static Deadline after(Clock clock, Duration timeout) {
        Objects.requireNonNull(clock, "clock must not be null");
        Arguments.notNegative(timeout, "timeout");
        return new Deadline(clock.instant().plus(timeout));
    }

    public Instant instant() {
        return instant;
    }

    public boolean isExpired(Clock clock) {
        Objects.requireNonNull(clock, "clock must not be null");
        return !clock.instant().isBefore(instant);
    }

    public Duration remaining(Clock clock) {
        Objects.requireNonNull(clock, "clock must not be null");
        Duration remaining = Duration.between(clock.instant(), instant);
        return remaining.isNegative() ? Duration.ZERO : remaining;
    }
}
