package com.xjtu.iron.foundation.reflection;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GenericTypeTest {

    @Test
    void shouldCaptureGenericType() {
        GenericType<List<String>> type = new GenericType<>() { };
        assertEquals(List.class, type.getRawClass());
        assertEquals("java.util.List<java.lang.String>", type.getType().getTypeName());
    }
}
