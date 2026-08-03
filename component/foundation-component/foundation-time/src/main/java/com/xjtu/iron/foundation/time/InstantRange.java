package com.xjtu.iron.foundation.time;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * 表示左闭右开的绝对时间范围。
 */
public final class InstantRange {

    /** 绝对时间范围起点，包含该时间点。 */
    private final Instant startInclusive;
    /** 绝对时间范围终点，不包含该时间点。 */
    private final Instant endExclusive;

    public InstantRange(Instant startInclusive, Instant endExclusive) {
        this.startInclusive = Objects.requireNonNull(startInclusive, "startInclusive must not be null");
        this.endExclusive = Objects.requireNonNull(endExclusive, "endExclusive must not be null");
        if (!startInclusive.isBefore(endExclusive)) {
            throw new IllegalArgumentException("startInclusive must be before endExclusive");
        }
    }

    public boolean contains(Instant value) {
        return value != null && !value.isBefore(startInclusive) && value.isBefore(endExclusive);
    }

    public boolean overlaps(InstantRange other) {
        return other != null
                && startInclusive.isBefore(other.endExclusive)
                && other.startInclusive.isBefore(endExclusive);
    }

    public Duration duration() {
        return Duration.between(startInclusive, endExclusive);
    }

    public Instant getStartInclusive() { return startInclusive; }
    public Instant getEndExclusive() { return endExclusive; }
}
