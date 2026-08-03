package com.xjtu.iron.foundation.core.number;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NumberConversionsTest {

    @Test
    void shouldCheckLongToIntOverflow() {
        assertEquals(10, NumberConversions.toIntExact(10L));
        assertThrows(ArithmeticException.class, () -> NumberConversions.toIntExact(Long.MAX_VALUE));
    }
}
