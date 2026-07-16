package com.xjtu.iron.distributed.lock.provider.redis;

import com.xjtu.iron.distributed.lock.api.LockOptions;
import com.xjtu.iron.distributed.lock.core.spi.request.*;
import com.xjtu.iron.distributed.lock.core.spi.response.*;
import com.xjtu.iron.distributed.lock.core.spi.status.*;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class RedisLockProviderTest {
    @Test
    void acquireSuccessShouldReturnLease() {
        RedisLockProvider provider = new RedisLockProvider((script, keys, args) -> Arrays.asList(1L, ""));
        LockAcquireResponse response = provider.acquire(LockAcquireRequest.builder()
                .lockName("job:1").ownerToken("token").options(LockOptions.noWait(Duration.ofSeconds(30))).build());
        assertEquals(LockAcquireStatus.ACQUIRED, response.getStatus());
        assertNotNull(response.getLease());
    }

    @Test
    void acquireWithFencingShouldReturnFencingToken() {
        RedisLockProvider provider = new RedisLockProvider((script, keys, args) -> {
            assertEquals("1", args.get(2));
            return Arrays.asList(1L, 101L);
        });
        LockAcquireResponse response = provider.acquire(LockAcquireRequest.builder()
                .lockName("job:fencing:1")
                .ownerToken("token")
                .options(LockOptions.builder()
                        .leaseTime(Duration.ofSeconds(30))
                        .fencingRequired(true)
                        .build())
                .build());
        assertEquals(LockAcquireStatus.ACQUIRED, response.getStatus());
        assertTrue(response.getLease().fencingToken().isPresent());
        assertEquals(101L, response.getLease().fencingToken().getAsLong());
        assertTrue(provider.capabilities().isFencingTokenSupported());
    }

    @Test
    void releaseNotOwnerShouldMapToNotOwner() {
        RedisLockProvider provider = new RedisLockProvider((script, keys, args) -> 0L);
        LockReleaseResponse response = provider.release(LockReleaseRequest.builder()
                .namespace("ns").lockName("job:1").lockKey("k").ownerToken("t").build());
        assertEquals(LockReleaseStatus.NOT_OWNER, response.getStatus());
    }
}
