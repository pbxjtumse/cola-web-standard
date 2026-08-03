package com.xjtu.iron.foundation.time;

import java.time.Clock;
import java.time.ZoneId;
import java.util.Objects;

/**
 * 基于 JDK 系统时钟的默认实现。
 */
public final class SystemClockProvider implements ClockProvider {

    /** 实际提供当前时间的 JDK Clock。 */
    private final Clock clock;

    public SystemClockProvider() {
        this(Clock.systemUTC());
    }

    public SystemClockProvider(ZoneId zoneId) {
        this(Clock.system(Objects.requireNonNull(zoneId, "zoneId must not be null")));
    }

    public SystemClockProvider(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public Clock clock() {
        return clock;
    }
}
