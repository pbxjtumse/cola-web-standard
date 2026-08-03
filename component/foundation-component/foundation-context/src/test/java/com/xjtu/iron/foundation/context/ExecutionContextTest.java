package com.xjtu.iron.foundation.context;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ExecutionContextTest {

    @Test
    void shouldCreateImmutableMutationCopy() {
        ExecutionContext original = ExecutionContext.builder()
                .put(StandardContextKeys.REQUEST_ID, "r1")
                .build();
        ExecutionContext changed = original.mutate()
                .put(StandardContextKeys.TENANT_ID, "tenant-a")
                .build();
        assertFalse(original.contains(StandardContextKeys.TENANT_ID));
        assertEquals("tenant-a", changed.get(StandardContextKeys.TENANT_ID).orElseThrow());
    }
}
