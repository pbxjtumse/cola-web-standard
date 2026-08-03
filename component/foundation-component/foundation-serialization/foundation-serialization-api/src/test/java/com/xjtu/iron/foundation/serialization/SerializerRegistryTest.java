package com.xjtu.iron.foundation.serialization;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SerializerRegistryTest {

    @Test
    void shouldRejectDuplicateFormats() {
        Serializer serializer = new NoopSerializer();
        assertThrows(IllegalArgumentException.class,
                () -> new SerializerRegistry(List.of(serializer, serializer)));
        assertEquals(serializer, new SerializerRegistry(List.of(serializer)).require(SerializationFormat.JSON));
    }

    private static final class NoopSerializer implements Serializer {
        public SerializationFormat format() { return SerializationFormat.JSON; }
        public byte[] serialize(Object value, SerializationContext context, SerializationOptions options) { return new byte[0]; }
        public <T> T deserialize(byte[] content, TypeDescriptor<T> targetType,
                                 SerializationContext context, SerializationOptions options) { return null; }
    }
}
