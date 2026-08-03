package com.xjtu.iron.foundation.resource;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceLocationTest {

    @Test
    void shouldParseClasspathLocation() {
        ResourceLocation location = ResourceLocation.of("classpath:config/retry.yml");
        assertTrue(location.isClasspath());
        assertEquals("config/retry.yml", location.path());
    }
}
