package com.xjtu.iron.distributed.lock.core;

import com.xjtu.iron.distributed.lock.api.*;
import com.xjtu.iron.distributed.lock.core.event.NoOpLockEventPublisher;
import com.xjtu.iron.distributed.lock.core.metrics.NoOpLockMetricsRecorder;
import com.xjtu.iron.distributed.lock.core.name.DefaultLockNamePatternResolver;
import com.xjtu.iron.distributed.lock.core.name.DefaultLockNameValidator;
import com.xjtu.iron.distributed.lock.core.registry.DefaultLockProviderRegistry;
import com.xjtu.iron.distributed.lock.core.spi.*;
import com.xjtu.iron.distributed.lock.core.spi.model.LockLease;
import com.xjtu.iron.distributed.lock.core.spi.request.*;
import com.xjtu.iron.distributed.lock.core.spi.response.*;
import com.xjtu.iron.distributed.lock.core.token.DefaultOwnerTokenGenerator;
import com.xjtu.iron.distributed.lock.core.wait.LockWaiterFactory;
import com.xjtu.iron.distributed.lock.core.watchdog.NoOpLockWatchdog;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class DefaultDistributedLockClientTest {

    @Test
    void tryLockNoWaitNotAcquiredShouldUseAcquireStage() {
        DistributedLockClient client = client(new FakeProvider(false, LockReleaseResponse.released()));
        LockResult<LockHandle> result = client.tryLock("job:1", LockOptions.noWait());
        assertEquals(LockStatus.NOT_ACQUIRED, result.status());
        assertEquals(LockStage.ACQUIRE, result.stage());
    }

    @Test
    void executeManualUnlockInsideCallbackShouldRemainSuccess() {
        FakeProvider provider = new FakeProvider(true, LockReleaseResponse.released());
        DistributedLockClient client = client(provider);
        LockResult<String> result = client.execute("job:1", LockOptions.noWait(), handle -> {
            assertTrue(handle.unlock());
            return "ok";
        });
        assertEquals(LockStatus.SUCCESS, result.status());
        assertEquals("ok", result.value().orElse(null));
        assertEquals(1, provider.releaseCount);
    }

    private static DistributedLockClient client(FakeProvider provider) {
        return new DefaultDistributedLockClient(
                new DefaultLockProviderRegistry("redis", Collections.singletonList(provider)),
                new DefaultOwnerTokenGenerator(), new LockWaiterFactory(), new NoOpLockWatchdog(),
                new NoOpLockEventPublisher(), new NoOpLockMetricsRecorder(), new DefaultLockNameValidator(),
                new DefaultLockNamePatternResolver(), Clock.systemUTC());
    }

    private static final class FakeProvider implements LockProvider {
        private final boolean acquire;
        private final LockReleaseResponse releaseResponse;
        private int releaseCount;
        private FakeProvider(boolean acquire, LockReleaseResponse releaseResponse) { this.acquire = acquire; this.releaseResponse = releaseResponse; }
        @Override public String providerName() { return "redis"; }
        @Override public LockAcquireResponse acquire(LockAcquireRequest request) {
            if (!acquire) { return LockAcquireResponse.notAcquired(Duration.ofSeconds(1)); }
            LockLease lease = LockLease.builder().providerName("redis").namespace(request.getNamespace())
                    .lockName(request.getLockName()).lockKey("key").ownerToken(request.getOwnerToken())
                    .leaseTime(request.getOptions().getLeaseTime()).acquiredAt(Instant.now()).build();
            return LockAcquireResponse.acquired(lease);
        }
        @Override public LockReleaseResponse release(LockReleaseRequest request) { releaseCount++; return releaseResponse; }
        @Override public LockRenewResponse renew(LockRenewRequest request) { return LockRenewResponse.renewed(Instant.now()); }
        @Override public LockCheckResponse check(LockCheckRequest request) { return LockCheckResponse.held(); }
        @Override public LockProviderCapabilities capabilities() { return LockProviderCapabilities.builder().autoRenewSupported(true).build(); }
    }
}
