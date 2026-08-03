package com.xjtu.iron.foundation.core.text;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TextMaskerTest {

    @Test
    void shouldMaskCommonIdentifiers() {
        assertEquals("138****5678", TextMasker.maskMobile("13812345678"));
        assertEquals("p***@example.com", TextMasker.maskEmail("person@example.com"));
    }
}
