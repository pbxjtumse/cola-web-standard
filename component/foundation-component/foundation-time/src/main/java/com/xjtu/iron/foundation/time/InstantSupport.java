package com.xjtu.iron.foundation.time;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * 提供绝对时间精度处理。
 */
public final class InstantSupport {

    private InstantSupport() {
    }

    public static Instant truncate(Instant value, TemporalPrecision precision) {
        if (value == null) {
            return null;
        }
        if (precision == null) {
            throw new IllegalArgumentException("precision must not be null");
        }
        ChronoUnit unit = precision.getUnit();
        return value.truncatedTo(unit);
    }
}
