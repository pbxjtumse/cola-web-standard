package com.xjtu.iron.distributed.lock.core;

import com.xjtu.iron.distributed.lock.core.event.NoOpLockEventPublisher;
import com.xjtu.iron.distributed.lock.core.metrics.LockMetricsFacade;
import com.xjtu.iron.distributed.lock.core.metrics.NoOpLockMetricsRecorder;
import com.xjtu.iron.distributed.lock.core.name.DefaultLockNamePatternResolver;
import com.xjtu.iron.distributed.lock.core.event.LockEventFactory;
import com.xjtu.iron.distributed.lock.core.result.LockReleaseOutcome;
import com.xjtu.iron.distributed.lock.core.runtime.LockRuntimeState;
import com.xjtu.iron.distributed.lock.core.spi.LockProvider;
import com.xjtu.iron.distributed.lock.core.spi.LockProviderCapabilities;
import com.xjtu.iron.distributed.lock.core.spi.model.LockLease;
import com.xjtu.iron.distributed.lock.core.spi.request.*;
import com.xjtu.iron.distributed.lock.core.spi.response.*;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class DefaultLockHandleTest {

    @Test
    void duplicateReleaseShouldReturnAlreadyAttemptedOutcome() {
        FakeProvider provider = new FakeProvider(LockReleaseResponse.released());
        DefaultLockHandle handle = newHandle(provider);
        assertTrue(handle.releaseWithOutcome().isReleased());
        LockReleaseOutcome second = handle.releaseWithOutcome();
        assertTrue(second.isAlreadyAttempted());
        assertEquals(1, provider.releaseCount);
    }

    @Test
    void releaseNotOwnerShouldMarkLost() {
        DefaultLockHandle handle = newHandle(new FakeProvider(LockReleaseResponse.notOwner()));
        LockReleaseOutcome outcome = handle.releaseWithOutcome();
        assertTrue(outcome.isLockLost());
        assertTrue(handle.isLost());
        assertTrue(handle.isReleaseAttempted());
    }

    private static DefaultLockHandle newHandle(LockProvider provider) {
        LockLease lease = LockLease.builder()
                .providerName("redis").namespace("ns").lockName("job:1").lockKey("key")
                .ownerToken("token").leaseTime(Duration.ofSeconds(30)).acquiredAt(Instant.now()).build();
        return new DefaultLockHandle(provider, lease, new LockRuntimeState(), new NoOpLockEventPublisher(),
                new LockEventFactory(), new LockMetricsFacade(new NoOpLockMetricsRecorder(), new DefaultLockNamePatternResolver()));
    }

    private static final class FakeProvider implements LockProvider {
        private final LockReleaseResponse releaseResponse;
        private int releaseCount;
        private FakeProvider(LockReleaseResponse releaseResponse) { this.releaseResponse = releaseResponse; }
        @Override public String providerName() { return "redis"; }
        @Override public LockAcquireResponse acquire(LockAcquireRequest request) { throw new UnsupportedOperationException(); }
        @Override public LockReleaseResponse release(LockReleaseRequest request) { releaseCount++; return releaseResponse; }
        @Override public LockRenewResponse renew(LockRenewRequest request) { return LockRenewResponse.renewed(Instant.now()); }
        @Override public LockCheckResponse check(LockCheckRequest request) { return LockCheckResponse.held(); }
        @Override public LockProviderCapabilities capabilities() { return LockProviderCapabilities.builder().autoRenewSupported(true).build(); }
    }
}
