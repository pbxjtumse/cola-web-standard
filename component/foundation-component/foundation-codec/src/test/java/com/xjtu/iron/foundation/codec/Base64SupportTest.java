package com.xjtu.iron.foundation.codec;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class Base64SupportTest {

    @Test
    void shouldRoundTripUtf8Text() {
        assertEquals("基础组件", Base64Support.decodeText(Base64Support.encodeText("基础组件")));
    }
}
