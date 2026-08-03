package com.xjtu.iron.foundation.time;

import java.time.LocalTime;
import java.util.Objects;

/**
 * 表示同一自然日内左闭右开的本地时间范围。
 */
public final class TimeRange {

    /** 本地时间范围起点，包含该时间。 */
    private final LocalTime startInclusive;
    /** 本地时间范围终点，不包含该时间。 */
    private final LocalTime endExclusive;

    public TimeRange(LocalTime startInclusive, LocalTime endExclusive) {
        this.startInclusive = Objects.requireNonNull(startInclusive, "startInclusive must not be null");
        this.endExclusive = Objects.requireNonNull(endExclusive, "endExclusive must not be null");
        if (!startInclusive.isBefore(endExclusive)) {
            throw new IllegalArgumentException("startInclusive must be before endExclusive");
        }
    }

    public boolean contains(LocalTime value) {
        return value != null && !value.isBefore(startInclusive) && value.isBefore(endExclusive);
    }

    public LocalTime getStartInclusive() { return startInclusive; }
    public LocalTime getEndExclusive() { return endExclusive; }
}
