package com.xjtu.iron.distributed.lock.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LockStatusStageRulesTest {

    @Test
    void validCombinationsShouldPass() {
        assertTrue(LockStatusStageRules.isValid(LockStatus.NOT_ACQUIRED, LockStage.ACQUIRE, false));
        assertTrue(LockStatusStageRules.isValid(LockStatus.NOT_ACQUIRED, LockStage.WAIT, false));
        assertTrue(LockStatusStageRules.isValid(LockStatus.RELEASE_FAILED, LockStage.RELEASE, true));
    }

    @Test
    void invalidCombinationsShouldFail() {
        assertFalse(LockStatusStageRules.isValid(LockStatus.SUCCESS, LockStage.RELEASE, true));
        assertFalse(LockStatusStageRules.isValid(LockStatus.PROVIDER_ERROR, LockStage.RELEASE, true));
        assertThrows(IllegalArgumentException.class, () -> LockResult.builder()
                .status(LockStatus.SUCCESS)
                .stage(LockStage.ACQUIRE)
                .acquired(true)
                .build());
    }
}
