package com.xjtu.iron.foundation.time;

import java.time.temporal.ChronoUnit;

/**
 * 定义组件工程常用的时间精度。
 */
public enum TemporalPrecision {
    MILLIS(ChronoUnit.MILLIS),
    SECONDS(ChronoUnit.SECONDS),
    MINUTES(ChronoUnit.MINUTES),
    HOURS(ChronoUnit.HOURS),
    DAYS(ChronoUnit.DAYS);

    /** 当前精度映射到的 JDK 时间单位。 */
    private final ChronoUnit unit;

    TemporalPrecision(ChronoUnit unit) {
        this.unit = unit;
    }

    public ChronoUnit getUnit() {
        return unit;
    }
}
