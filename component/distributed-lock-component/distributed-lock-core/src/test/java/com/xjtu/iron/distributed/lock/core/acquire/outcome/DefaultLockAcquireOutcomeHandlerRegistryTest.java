package com.xjtu.iron.distributed.lock.core.acquire.outcome;

import com.xjtu.iron.distributed.lock.api.LockHandle;
import com.xjtu.iron.distributed.lock.api.LockResult;
import com.xjtu.iron.distributed.lock.core.spi.status.LockAcquireStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultLockAcquireOutcomeHandlerRegistryTest {

    @Test
    void shouldRejectMissingStatusHandler() {
        assertThatThrownBy(() -> new DefaultLockAcquireOutcomeHandlerRegistry(List.of(
                handler(LockAcquireStatus.ACQUIRED), handler(LockAcquireStatus.NOT_ACQUIRED))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PROVIDER_ERROR");
    }

    @Test
    void shouldRejectDuplicateStatusHandler() {
        assertThatThrownBy(() -> new DefaultLockAcquireOutcomeHandlerRegistry(List.of(
                handler(LockAcquireStatus.ACQUIRED), handler(LockAcquireStatus.ACQUIRED),
                handler(LockAcquireStatus.NOT_ACQUIRED), handler(LockAcquireStatus.PROVIDER_ERROR))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duplicate");
    }

    private LockAcquireOutcomeHandler handler(LockAcquireStatus status) {
        return new LockAcquireOutcomeHandler() {
            @Override public LockAcquireStatus status() { return status; }
            @Override public LockResult<LockHandle> handle(LockAcquireOutcomeContext context) {
                throw new UnsupportedOperationException();
            }
        };
    }
}
