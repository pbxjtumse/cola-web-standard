package com.xjtu.iron.distributed.lock.provider.redisson;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RedissonOwnershipRegistryTest {

    @Test
    void shouldReserveFindAndRemoveOwnership() {
        RedissonOwnershipRegistry registry = new RedissonOwnershipRegistry();
        RedissonOwnershipRegistry.Reservation reservation = registry.reserve("owner-1", "lock-a", 101L);

        assertTrue(reservation.isReserved());
        assertEquals(101L, registry.find("owner-1").threadId());
        assertEquals("lock-a", registry.find("owner-1").lockKey());
        assertEquals(1, registry.size());

        registry.remove("owner-1");
        assertNull(registry.find("owner-1"));
        assertEquals(0, registry.size());
    }

    @Test
    void sameThreadAndLockMustNotBeReservedTwice() {
        RedissonOwnershipRegistry registry = new RedissonOwnershipRegistry();
        assertTrue(registry.reserve("owner-1", "lock-a", 7L).isReserved());

        RedissonOwnershipRegistry.Reservation second = registry.reserve("owner-2", "lock-a", 7L);
        assertFalse(second.isReserved());
        assertEquals("owner-1", second.conflictOwnerToken());
    }

    @Test
    void sameThreadCanOwnDifferentLocks() {
        RedissonOwnershipRegistry registry = new RedissonOwnershipRegistry();
        assertTrue(registry.reserve("owner-1", "lock-a", 7L).isReserved());
        assertTrue(registry.reserve("owner-2", "lock-b", 7L).isReserved());
    }
}
