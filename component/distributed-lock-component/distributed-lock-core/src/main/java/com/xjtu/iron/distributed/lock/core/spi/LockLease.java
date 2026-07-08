package com.xjtu.iron.distributed.lock.core.spi;


import java.time.Duration;
import java.time.Instant;

public final class LockLease {

    private final String lockName;

    private final String lockKey;

    private final String ownerToken;

    private final Long fencingToken;

    private final Duration leaseTime;

    private final Instant acquiredAt;

    private final Instant expireAt;

    private volatile boolean lost;

    private volatile boolean released;

    // getters / markLost / markReleased
}
