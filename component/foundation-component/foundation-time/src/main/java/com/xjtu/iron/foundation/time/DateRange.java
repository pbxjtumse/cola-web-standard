package com.xjtu.iron.foundation.time;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 表示左闭右闭的本地日期范围。
 */
public final class DateRange {

    /** 日期范围起点，包含该日期。 */
    private final LocalDate startInclusive;
    /** 日期范围终点，包含该日期。 */
    private final LocalDate endInclusive;

    public DateRange(LocalDate startInclusive, LocalDate endInclusive) {
        this.startInclusive = Objects.requireNonNull(startInclusive, "startInclusive must not be null");
        this.endInclusive = Objects.requireNonNull(endInclusive, "endInclusive must not be null");
        if (startInclusive.isAfter(endInclusive)) {
            throw new IllegalArgumentException("startInclusive must not be after endInclusive");
        }
    }

    public LocalDate getStartInclusive() { return startInclusive; }
    public LocalDate getEndInclusive() { return endInclusive; }

    public boolean contains(LocalDate value) {
        return value != null && !value.isBefore(startInclusive) && !value.isAfter(endInclusive);
    }

    public boolean overlaps(DateRange other) {
        return other != null
                && !endInclusive.isBefore(other.startInclusive)
                && !other.endInclusive.isBefore(startInclusive);
    }

    /** 返回范围内的全部日期。 */
    public List<LocalDate> dates() {
        List<LocalDate> result = new ArrayList<>();
        LocalDate current = startInclusive;
        while (!current.isAfter(endInclusive)) {
            result.add(current);
            current = current.plusDays(1);
        }
        return List.copyOf(result);
    }
}
