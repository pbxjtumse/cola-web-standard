package com.xjtu.iron.retry.core.time;

import com.xjtu.iron.foundation.test.time.MutableClockProvider;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** 验证重试时钟复用 Foundation 墙上时钟并保留独立单调时间。 */
class SystemRetryClockTest {

    @Test
    void shouldCombineFoundationClockAndMonotonicNanoSource() {
        MutableClockProvider clockProvider = new MutableClockProvider(
                Instant.parse("2026-08-03T00:00:00Z"),
                ZoneOffset.UTC
        );
        AtomicLong nanoTime = new AtomicLong(123L);
        RetryClock retryClock = new SystemRetryClock(
                clockProvider,
                nanoTime::get
        );

        assertEquals(Instant.parse("2026-08-03T00:00:00Z"), retryClock.now());
        assertEquals(123L, retryClock.nanoTime());

        clockProvider.advance(Duration.ofSeconds(2));
        nanoTime.set(2_000_000_123L);

        assertEquals(Instant.parse("2026-08-03T00:00:02Z"), retryClock.now());
        assertEquals(2_000_000_123L, retryClock.nanoTime());
    }
}
