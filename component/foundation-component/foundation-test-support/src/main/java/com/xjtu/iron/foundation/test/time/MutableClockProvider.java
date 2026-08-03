package com.xjtu.iron.foundation.test.time;

import com.xjtu.iron.foundation.time.ClockProvider;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;

/**
 * 可在测试中手动推进的线程安全时钟。
 */
public final class MutableClockProvider implements ClockProvider {

    /** 测试环境当前绝对时间点。 */
    private Instant instant;
    /** 测试时钟使用的时区。 */
    private ZoneId zoneId;

    public MutableClockProvider(Instant instant, ZoneId zoneId) {
        this.instant = Objects.requireNonNull(instant, "instant must not be null");
        this.zoneId = Objects.requireNonNull(zoneId, "zoneId must not be null");
    }

    @Override
    public synchronized Clock clock() {
        return Clock.fixed(instant, zoneId);
    }

    /** 推进当前测试时间。 */
    public synchronized void advance(Duration duration) {
        if (duration == null || duration.isNegative()) {
            throw new IllegalArgumentException("duration must not be null or negative");
        }
        instant = instant.plus(duration);
    }

    /** 将测试时间直接设置为指定时间点。 */
    public synchronized void setInstant(Instant instant) {
        this.instant = Objects.requireNonNull(instant, "instant must not be null");
    }
}
