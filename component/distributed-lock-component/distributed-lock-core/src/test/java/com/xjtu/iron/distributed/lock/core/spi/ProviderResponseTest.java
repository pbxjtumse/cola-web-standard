package com.xjtu.iron.distributed.lock.core.spi;

import com.xjtu.iron.distributed.lock.core.spi.response.LockReleaseResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProviderResponseTest {
    @Test
    void providerErrorShouldBeUnified() {
        RuntimeException error = new RuntimeException("redis down");
        LockReleaseResponse response = LockReleaseResponse.failed(error);
        assertTrue(response.hasProviderError());
        assertSame(error, response.getError());
        assertEquals("redis down", response.getMessage());
    }
}
