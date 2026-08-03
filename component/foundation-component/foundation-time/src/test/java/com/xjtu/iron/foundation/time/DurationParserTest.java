package com.xjtu.iron.foundation.time;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DurationParserTest {

    @Test
    void shouldParseSimpleAndIsoDurations() {
        assertEquals(Duration.ofMillis(100), DurationParser.parse("100ms"));
        assertEquals(Duration.ofMinutes(5), DurationParser.parse("5m"));
        assertEquals(Duration.ofSeconds(5), DurationParser.parse("PT5S"));
    }
}
