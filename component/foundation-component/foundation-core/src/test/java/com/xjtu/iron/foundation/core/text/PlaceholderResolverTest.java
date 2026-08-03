package com.xjtu.iron.foundation.core.text;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PlaceholderResolverTest {

    @Test
    void shouldResolveKnownVariables() {
        assertEquals("message-order", PlaceholderResolver.resolve("${type}-${name}",
                Map.of("type", "message", "name", "order"), false));
    }

    @Test
    void shouldRejectUnknownVariableInStrictMode() {
        assertThrows(IllegalArgumentException.class,
                () -> PlaceholderResolver.resolve("${missing}", Map.of(), false));
    }
}
