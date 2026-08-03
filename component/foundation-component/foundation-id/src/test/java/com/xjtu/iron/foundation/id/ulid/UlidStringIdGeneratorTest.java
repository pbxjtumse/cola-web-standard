package com.xjtu.iron.foundation.id.ulid;

import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UlidStringIdGeneratorTest {

    @Test
    void shouldGenerateMonotonicUlid() {
        Clock clock = Clock.fixed(Instant.ofEpochMilli(1_800_000_000_000L), ZoneOffset.UTC);
        UlidStringIdGenerator generator = new UlidStringIdGenerator(
                clock,
                new SecureRandom(new byte[]{5, 6, 7, 8})
        );

        String first = generator.nextId();
        String second = generator.nextId();

        assertEquals(26, first.length());
        assertTrue(first.compareTo(second) < 0);
    }
}
