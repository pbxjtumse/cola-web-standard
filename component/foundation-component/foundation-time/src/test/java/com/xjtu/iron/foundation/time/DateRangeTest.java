package com.xjtu.iron.foundation.time;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DateRangeTest {

    @Test
    void shouldEnumerateInclusiveDates() {
        DateRange range = new DateRange(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 3));
        assertEquals(List.of(
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 2),
                LocalDate.of(2026, 8, 3)
        ), range.dates());
        assertTrue(range.contains(LocalDate.of(2026, 8, 2)));
    }
}
