package com.xjtu.iron.foundation.serialization.jackson;

import com.xjtu.iron.foundation.reflection.GenericType;
import com.xjtu.iron.foundation.serialization.SerializationOptions;
import com.xjtu.iron.foundation.serialization.TypeDescriptor;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JacksonJsonSerializerTest {

    @Test
    void shouldRoundTripJavaTimeAndGenericCollection() {
        JacksonJsonSerializer serializer = JacksonSerializerFactory.createDefault();
        Sample value = new Sample("message-1", Instant.parse("2026-08-01T00:00:00Z"));
        byte[] content = serializer.serialize(List.of(value));

        TypeDescriptor<List<Sample>> type = TypeDescriptor.of(new GenericType<List<Sample>>() { });
        assertEquals(List.of(value), serializer.deserialize(content, type));
    }

    @Test
    void shouldEnforceMaximumPayloadSize() {
        JacksonJsonSerializer serializer = JacksonSerializerFactory.createDefault();
        SerializationOptions options = SerializationOptions.builder().maxBytes(5).build();
        assertThrows(RuntimeException.class,
                () -> serializer.serialize("content", com.xjtu.iron.foundation.serialization.SerializationContext.empty(), options));
    }

    public static final class Sample {
        private String id;
        private Instant createdAt;

        public Sample() { }
        public Sample(String id, Instant createdAt) { this.id = id; this.createdAt = createdAt; }
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public Instant getCreatedAt() { return createdAt; }
        public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

        @Override
        public boolean equals(Object object) {
            return object instanceof Sample other && Objects.equals(id, other.id) && Objects.equals(createdAt, other.createdAt);
        }

        @Override
        public int hashCode() { return Objects.hash(id, createdAt); }
    }
}
