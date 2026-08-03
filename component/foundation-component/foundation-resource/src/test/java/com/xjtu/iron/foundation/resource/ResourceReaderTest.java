package com.xjtu.iron.foundation.resource;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ResourceReaderTest {

    @Test
    void shouldReadLimitedMemoryResource() {
        Resource resource = new ByteArrayResource("hello".getBytes(StandardCharsets.UTF_8), "sample");
        assertEquals("hello", ResourceReader.readText(resource, 10));
        assertThrows(ResourceLimitExceededException.class, () -> ResourceReader.readBytes(resource, 2));
    }
}
