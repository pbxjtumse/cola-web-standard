package com.xjtu.iron.foundation.id.snowflake;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SnowflakeLongIdGeneratorTest {

    @Test
    void shouldGenerateIncreasingIdsInSameMillisecond() {
        long timestamp = SnowflakeOptions.DEFAULT_EPOCH_MILLIS + 10_000L;
        Clock clock = Clock.fixed(Instant.ofEpochMilli(timestamp), ZoneOffset.UTC);
        SnowflakeLongIdGenerator generator = new SnowflakeLongIdGenerator(
                SnowflakeOptions.builder(7L).build(),
                clock
        );

        long first = generator.nextLongId();
        long second = generator.nextLongId();

        assertTrue(first < second);
    }

    @Test
    void shouldRejectWorkerIdOutsideTenBitRange() {
        assertThrows(
                IllegalArgumentException.class,
                () -> SnowflakeOptions.builder(1024L).build()
        );
    }

    @Test
    void shouldFailFastWhenClockMovesBackwards() {
        long timestamp = SnowflakeOptions.DEFAULT_EPOCH_MILLIS + 10_000L;
        MutableClock clock = new MutableClock(timestamp);
        SnowflakeLongIdGenerator generator = new SnowflakeLongIdGenerator(
                SnowflakeOptions.builder(7L).build(),
                clock
        );
        generator.nextLongId();
        clock.currentMillis = timestamp - 1L;

        assertThrows(
                com.xjtu.iron.foundation.id.api.IdGenerationException.class,
                generator::nextLongId
        );
    }

    private static final class MutableClock extends Clock {

        private long currentMillis;

        private MutableClock(long currentMillis) {
            this.currentMillis = currentMillis;
        }

        @Override
        public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return Instant.ofEpochMilli(currentMillis);
        }

        @Override
        public long millis() {
            return currentMillis;
        }
    }
}
