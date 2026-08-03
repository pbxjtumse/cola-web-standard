package com.xjtu.iron.foundation.time;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class DeadlineTest {

    @Test
    void remainingShouldReturnZeroWhenExpired() {
        Clock clock = Clock.fixed(Instant.parse("2026-08-03T00:00:00Z"), ZoneOffset.UTC);
        Deadline deadline = Deadline.after(clock, Duration.ZERO);
        assertThat(deadline.remaining(clock)).isZero();
    }
}
