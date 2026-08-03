package com.xjtu.iron.foundation.id.uuid;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UuidV4StringIdGeneratorTest {

    @Test
    void shouldGenerateStandardVersionFourUuid() {
        UUID uuid = UUID.fromString(new UuidV4StringIdGenerator().nextId());

        assertEquals(4, uuid.version());
        assertEquals(2, uuid.variant());
    }
}
