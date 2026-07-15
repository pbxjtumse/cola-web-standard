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
        FakeHandle handle = new FakeHandle();
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

    private static final class FakeHandle implements WatchdogLockHandle {
        private final AtomicBoolean lost = new AtomicBoolean();
        @Override public String watchdogId() { return "id"; }
        @Override public void markLostByWatchdog(String reason, Throwable error) { lost.set(true); }
        @Override public String lockName() { return "l"; }
        @Override public String lockKey() { return "k"; }
        @Override public String ownerToken() { return "t"; }
        @Override public OptionalLong fencingToken() { return OptionalLong.empty(); }
        @Override public Instant acquiredAt() { return Instant.now().minusMillis(80); }
        @Override public Duration leaseTime() { return Duration.ofMillis(60); }
        @Override public Instant expireAt() { return Instant.now().plusMillis(60); }
        @Override public boolean isLost() { return lost.get(); }
        @Override public boolean isReleaseAttempted() { return false; }
        @Override public boolean checkHeld() { return true; }
        @Override public boolean renew() { return true; }
        @Override public boolean unlock() { return true; }
        @Override public void assertHeld() { }
    }
}
