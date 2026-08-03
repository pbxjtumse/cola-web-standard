package com.xjtu.iron.foundation.id;

import com.xjtu.iron.foundation.time.ClockProvider;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TimeSortableIdGeneratorTest {

    @Test
    void shouldCreateTwentySixCharacterId() {
        ClockProvider provider = () -> Clock.fixed(Instant.parse("2026-08-01T00:00:00Z"), ZoneOffset.UTC);
        String id = new TimeSortableIdGenerator(provider, new SecureRandom(new byte[]{1, 2, 3})).nextId();
        assertEquals(26, id.length());
    }
}
