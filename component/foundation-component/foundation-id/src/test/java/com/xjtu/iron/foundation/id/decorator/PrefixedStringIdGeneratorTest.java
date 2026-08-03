package com.xjtu.iron.foundation.id.decorator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PrefixedStringIdGeneratorTest {

    @Test
    void shouldAddPrefixWithoutChangingDelegateContract() {
        PrefixedStringIdGenerator generator = new PrefixedStringIdGenerator(
                "retry_",
                () -> "123"
        );

        assertEquals("retry_123", generator.nextId());
    }
}
