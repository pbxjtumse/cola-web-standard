package com.xjtu.iron.foundation.time;

import java.time.Clock;
import java.util.Objects;

/**
 * 基于系统时间的 ClockProvider 默认实现。
 */
public final class SystemClockProvider implements ClockProvider {

    private final Clock clock;

    public SystemClockProvider() {
        this(Clock.systemUTC());
    }

    public SystemClockProvider(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public Clock clock() {
        return clock;
    }
}
