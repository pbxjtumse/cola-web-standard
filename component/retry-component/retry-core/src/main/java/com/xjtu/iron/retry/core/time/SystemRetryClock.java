package com.xjtu.iron.retry.core.time;

import com.xjtu.iron.foundation.time.ClockProvider;
import com.xjtu.iron.foundation.time.SystemClockProvider;

import java.time.Clock;
import java.util.Objects;
import java.util.function.LongSupplier;

/**
 * 组合 Foundation 墙上时钟与 JVM 单调时钟的默认重试时钟。
 */
public final class SystemRetryClock implements RetryClock {

    /** 为事件时间戳提供绝对时间。 */
    private final ClockProvider clockProvider;

    /** 为耗时和总时长预算提供单调时间。 */
    private final LongSupplier nanoTimeSource;

    public SystemRetryClock() {
        this(new SystemClockProvider(), System::nanoTime);
    }

    public SystemRetryClock(
            ClockProvider clockProvider,
            LongSupplier nanoTimeSource) {
        this.clockProvider = Objects.requireNonNull(
                clockProvider,
                "clockProvider must not be null"
        );
        this.nanoTimeSource = Objects.requireNonNull(
                nanoTimeSource,
                "nanoTimeSource must not be null"
        );
    }

    @Override
    public Clock clock() {
        return clockProvider.clock();
    }

    @Override
    public long nanoTime() {
        return nanoTimeSource.getAsLong();
    }
}
