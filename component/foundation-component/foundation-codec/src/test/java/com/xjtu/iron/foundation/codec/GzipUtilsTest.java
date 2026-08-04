package com.xjtu.iron.foundation.codec;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class IronGzipTest {

    @Test
    void decompressShouldRestoreOriginalBytes() {
        byte[] source = "hello".getBytes(StandardCharsets.UTF_8);
        assertThat(IronGzip.decompress(IronGzip.compress(source))).isEqualTo(source);
    }
}
