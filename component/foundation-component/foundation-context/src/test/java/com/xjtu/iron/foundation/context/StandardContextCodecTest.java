package com.xjtu.iron.foundation.context;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class StandardContextCodecTest {

    @Test
    void shouldWriteAndReadOnlyStandardKeys() {
        ContextKey<String> privateKey = ContextKey.of("private", String.class);
        ExecutionContext source = ExecutionContext.builder()
                .put(StandardContextKeys.REQUEST_ID, "request-1")
                .put(privateKey, "secret")
                .build();
        MapContextCarrier carrier = new MapContextCarrier();
        StandardContextCodec codec = new StandardContextCodec();
        codec.write(source, carrier);

        ExecutionContext restored = codec.read(carrier);
        assertEquals("request-1", restored.get(StandardContextKeys.REQUEST_ID).orElseThrow());
        assertFalse(restored.contains(privateKey));
    }
}
