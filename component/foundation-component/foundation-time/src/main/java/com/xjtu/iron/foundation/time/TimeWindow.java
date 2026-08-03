package com.xjtu.iron.foundation.time;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * 表示以基准时间为中心的前后容差窗口。
 */
public final class TimeWindow {

    /** 时间窗口的中心时间点。 */
    private final Instant center;
    /** 中心时间点之前允许的偏移。 */
    private final Duration before;
    /** 中心时间点之后允许的偏移。 */
    private final Duration after;

    public TimeWindow(Instant center, Duration before, Duration after) {
        this.center = Objects.requireNonNull(center, "center must not be null");
        if (before == null || after == null || before.isNegative() || after.isNegative()) {
            throw new IllegalArgumentException("before and after must not be null or negative");
        }
        this.before = before;
        this.after = after;
    }

    public InstantRange asRange() {
        return new InstantRange(center.minus(before), center.plus(after).plusNanos(1));
    }

    public boolean contains(Instant value) {
        return value != null
                && !value.isBefore(center.minus(before))
                && !value.isAfter(center.plus(after));
    }
}
