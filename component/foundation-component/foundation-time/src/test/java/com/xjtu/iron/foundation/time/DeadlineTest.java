package com.xjtu.iron.foundation.time;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class DeadlineTest {

    @Test
    void shouldCalculateRemainingDuration() {
        ClockProvider provider = () -> Clock.fixed(Instant.parse("2026-08-01T00:00:00Z"), ZoneOffset.UTC);
        Deadline deadline = Deadline.after(provider, Duration.ofSeconds(5));
        assertEquals(Duration.ofSeconds(5), deadline.remaining(provider));
        assertFalse(deadline.isExpired(provider));
    }
}
