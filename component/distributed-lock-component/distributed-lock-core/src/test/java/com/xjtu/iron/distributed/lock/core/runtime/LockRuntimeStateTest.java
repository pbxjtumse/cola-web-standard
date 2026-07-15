package com.xjtu.iron.distributed.lock.core.runtime;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LockRuntimeStateTest {

    @Test
    void lostAndReleaseAttemptedAreIndependentFlags() {
        LockRuntimeState state = new LockRuntimeState();
        assertTrue(state.markLostOnce());
        assertFalse(state.markLostOnce());
        assertTrue(state.markReleaseAttemptedOnce());
        assertFalse(state.markReleaseAttemptedOnce());
        assertTrue(state.isLost());
        assertTrue(state.isReleaseAttempted());
    }
}
