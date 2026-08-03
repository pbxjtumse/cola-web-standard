package com.xjtu.iron.foundation.core.text;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CaseConverterTest {

    @Test
    void shouldConvertBetweenCommonFormats() {
        assertEquals("retry_policy", CaseConverter.convert("retryPolicy", CaseFormat.LOWER_CAMEL, CaseFormat.LOWER_UNDERSCORE));
        assertEquals("RetryPolicy", CaseConverter.convert("retry_policy", CaseFormat.LOWER_UNDERSCORE, CaseFormat.UPPER_CAMEL));
    }
}
