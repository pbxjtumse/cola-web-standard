package com.xjtu.iron.foundation.time;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * 表示一个绝对截止时间。
 *
 * <p>适用于重试总超时、锁等待、任务执行和缓存刷新等技术场景。</p>
 */
public final class Deadline {

    /** 截止时间对应的绝对时间点。 */
    private final Instant instant;

    private Deadline(Instant instant) {
        this.instant = instant;
    }

    public static Deadline at(Instant instant) {
        return new Deadline(Objects.requireNonNull(instant, "instant must not be null"));
    }

    public static Deadline after(ClockProvider clockProvider, Duration duration) {
        Objects.requireNonNull(clockProvider, "clockProvider must not be null");
        if (duration == null || duration.isNegative()) {
            throw new IllegalArgumentException("duration must not be null or negative");
        }
        return new Deadline(clockProvider.now().plus(duration));
    }

    public Instant getInstant() {
        return instant;
    }

    /** 判断截止时间是否已经到达。 */
    public boolean isExpired(ClockProvider clockProvider) {
        return !clockProvider.now().isBefore(instant);
    }

    /** 返回剩余时间；已经到期时返回零。 */
    public Duration remaining(ClockProvider clockProvider) {
        Duration remaining = Duration.between(clockProvider.now(), instant);
        return remaining.isNegative() ? Duration.ZERO : remaining;
    }

    /** 判断等待指定时间后是否会越过截止时间。 */
    public boolean canWait(ClockProvider clockProvider, Duration delay) {
        if (delay == null || delay.isNegative()) {
            throw new IllegalArgumentException("delay must not be null or negative");
        }
        return !clockProvider.now().plus(delay).isAfter(instant);
    }
}
