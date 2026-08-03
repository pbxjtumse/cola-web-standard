package com.xjtu.iron.foundation.codec;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CompressionSupportTest {

    @Test
    void shouldRoundTripGzipContent() {
        byte[] source = "foundation-component".getBytes(StandardCharsets.UTF_8);
        assertArrayEquals(source, CompressionSupport.gunzip(CompressionSupport.gzip(source), 1024));
    }

    @Test
    void shouldRejectOversizedOutput() {
        byte[] compressed = CompressionSupport.gzip("0123456789".getBytes(StandardCharsets.UTF_8));
        assertThrows(IllegalArgumentException.class, () -> CompressionSupport.gunzip(compressed, 5));
    }
}
