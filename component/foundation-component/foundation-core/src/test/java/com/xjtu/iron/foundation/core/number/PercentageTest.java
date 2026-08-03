package com.xjtu.iron.foundation.core.number;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PercentageTest {

    @Test
    void shouldConvertBetweenPercentAndRatio() {
        Percentage percentage = Percentage.fromRatio(new BigDecimal("0.125"));
        assertEquals(new BigDecimal("12.5"), percentage.asPercent());
        assertEquals(new BigDecimal("0.1250"), percentage.asRatio(4, RoundingMode.HALF_UP));
    }
}
