package com.xjtu.iron.distributed.lock.api;


import java.time.Duration;
import java.time.Instant;
import java.util.OptionalLong;

public interface LockHandle extends AutoCloseable {

    String lockName();

    String lockKey();

    String ownerToken();

    OptionalLong fencingToken();

    Instant acquiredAt();

    Duration leaseTime();

    Instant expireAt();

    boolean isLost();

    boolean isReleased();

    boolean isHeld();

    boolean renew();

    boolean unlock();

    void assertHeld();

    @Override
    default void close() {
        unlock();
    }
}
