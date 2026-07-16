package com.xjtu.iron.distributed.lock.core.watchdog;

import com.xjtu.iron.distributed.lock.api.LockOptions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.OptionalLong;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ScheduledLockWatchdogTest {
    @Test
    void maxRenewTimeShouldMarkLostAndStop() throws Exception {
        ScheduledLockWatchdog watchdog = new ScheduledLockWatchdog(Executors.newSingleThreadScheduledExecutor());
        FakeHandle handle = new FakeHandle().acquiredAt(Instant.now().minusMillis(80));
        LockOptions options = LockOptions.builder()
                .leaseTime(Duration.ofMillis(60))
                .renewInterval(Duration.ofMillis(20))
                .autoRenew(true)
                .maxRenewTime(Duration.ofMillis(30))
                .build();
        watchdog.start(handle, options);
        Thread.sleep(120);
        assertTrue(handle.lost.get());
        watchdog.close();
    }

    @Test
    void releaseAttemptedShouldStopRenewing() throws Exception {
        ScheduledLockWatchdog watchdog = new ScheduledLockWatchdog(Executors.newSingleThreadScheduledExecutor());
        FakeHandle handle = new FakeHandle();
        LockOptions options = LockOptions.builder()
                .leaseTime(Duration.ofMillis(120))
                .renewInterval(Duration.ofMillis(20))
                .autoRenew(true)
                .maxRenewTime(Duration.ofSeconds(1))
                .build();
        watchdog.start(handle, options);
        Thread.sleep(55);
        handle.releaseAttempted.set(true);
        int renewCountAfterRelease = handle.renewCount.get();
        Thread.sleep(80);
        assertEquals(renewCountAfterRelease, handle.renewCount.get());
        watchdog.close();
    }

    @Test
    void lostHandleShouldStopRenewing() throws Exception {
        ScheduledLockWatchdog watchdog = new ScheduledLockWatchdog(Executors.newSingleThreadScheduledExecutor());
        FakeHandle handle = new FakeHandle();
        LockOptions options = LockOptions.builder()
                .leaseTime(Duration.ofMillis(120))
                .renewInterval(Duration.ofMillis(20))
                .autoRenew(true)
                .maxRenewTime(Duration.ofSeconds(1))
                .build();
        watchdog.start(handle, options);
        Thread.sleep(55);
        handle.lost.set(true);
        int renewCountAfterLost = handle.renewCount.get();
        Thread.sleep(80);
        assertEquals(renewCountAfterLost, handle.renewCount.get());
        watchdog.close();
    }

    private static final class FakeHandle implements WatchdogLockHandle {
        private final AtomicBoolean lost = new AtomicBoolean();
        private final AtomicBoolean releaseAttempted = new AtomicBoolean();
        private final AtomicInteger renewCount = new AtomicInteger();
        private Instant acquiredAt = Instant.now();

        FakeHandle acquiredAt(Instant acquiredAt) {
            this.acquiredAt = acquiredAt;
            return this;
        }

        @Override public String watchdogId() { return "id"; }
        @Override public void markLostByWatchdog(String reason, Throwable error) { lost.set(true); }
        @Override public String lockName() { return "l"; }
        @Override public String lockKey() { return "k"; }
        @Override public String ownerToken() { return "t"; }
        @Override public OptionalLong fencingToken() { return OptionalLong.empty(); }
        @Override public Instant acquiredAt() { return acquiredAt; }
        @Override public Duration leaseTime() { return Duration.ofMillis(60); }
        @Override public Instant expireAt() { return Instant.now().plusMillis(60); }
        @Override public boolean isLost() { return lost.get(); }
        @Override public boolean isReleaseAttempted() { return releaseAttempted.get(); }
        @Override public boolean checkHeld() { return true; }
        @Override public boolean renew() { renewCount.incrementAndGet(); return true; }
        @Override public boolean unlock() { releaseAttempted.set(true); return true; }
        @Override public void assertHeld() { }
    }
}
