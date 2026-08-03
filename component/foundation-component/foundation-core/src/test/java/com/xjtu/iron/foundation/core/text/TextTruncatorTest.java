package com.xjtu.iron.foundation.core.text;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TextTruncatorTest {

    @Test
    void shouldTruncateByUnicodeCodePoint() {
        assertEquals("A😀", TextTruncator.truncate("A😀BC", 2));
        assertEquals("A...", TextTruncator.truncateWithSuffix("ABCDE", 4, "..."));
    }
}
