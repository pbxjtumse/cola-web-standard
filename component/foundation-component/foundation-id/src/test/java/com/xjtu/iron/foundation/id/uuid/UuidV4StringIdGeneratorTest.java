package com.xjtu.iron.foundation.id.uuid;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class UuidV4StringIdGeneratorTest {

    @Test
    void shouldGenerateDifferentVersionFourIds() {
        UuidV4StringIdGenerator generator = new UuidV4StringIdGenerator();

        String first = generator.nextId();
        String second = generator.nextId();

        assertEquals(4, UUID.fromString(first).version());
        assertNotEquals(first, second);
    }
}
