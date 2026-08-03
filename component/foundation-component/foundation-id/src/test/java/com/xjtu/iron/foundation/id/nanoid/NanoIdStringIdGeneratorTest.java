package com.xjtu.iron.foundation.id.nanoid;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NanoIdStringIdGeneratorTest {

    @Test
    void shouldRespectSizeAndAlphabet() {
        NanoIdOptions options = NanoIdOptions.builder()
                .alphabet("abc123")
                .size(16)
                .build();
        NanoIdStringIdGenerator generator = new NanoIdStringIdGenerator(options);

        String id = generator.nextId();

        assertEquals(16, id.length());
        assertTrue(id.chars().allMatch(character -> "abc123".indexOf(character) >= 0));
    }

    @Test
    void shouldSupportSingleCharacterAlphabetWithoutLooping() {
        NanoIdStringIdGenerator generator = new NanoIdStringIdGenerator(
                NanoIdOptions.builder().alphabet("x").size(5).build()
        );

        assertEquals("xxxxx", generator.nextId());
    }

    @Test
    void shouldRejectDuplicateAlphabetCharacters() {
        assertThrows(
                IllegalArgumentException.class,
                () -> NanoIdOptions.builder().alphabet("aabc").build()
        );
    }
}
