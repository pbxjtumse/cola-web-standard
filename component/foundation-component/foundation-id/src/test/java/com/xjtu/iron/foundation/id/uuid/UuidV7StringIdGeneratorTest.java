package com.xjtu.iron.foundation.id.uuid;

import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UuidV7StringIdGeneratorTest {

    @Test
    void shouldGenerateVersionSevenAndMonotonicText() {
        Clock clock = Clock.fixed(Instant.ofEpochMilli(1_800_000_000_000L), ZoneOffset.UTC);
        SecureRandom random = new SecureRandom(new byte[]{1, 2, 3, 4});
        UuidV7StringIdGenerator generator = new UuidV7StringIdGenerator(clock, random);

        String first = generator.nextId();
        String second = generator.nextId();

        assertEquals(7, UUID.fromString(first).version());
        assertEquals(2, UUID.fromString(first).variant());
        assertTrue(first.compareTo(second) < 0);
    }
}
