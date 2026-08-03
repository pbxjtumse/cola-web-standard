package com.xjtu.iron.foundation.codec;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DigestSupportTest {

    @Test
    void shouldCalculateKnownSha256() {
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
                DigestSupport.sha256Hex("abc"));
    }
}
