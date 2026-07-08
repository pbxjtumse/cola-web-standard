package com.xjtu.iron.distributed.lock.api;


import java.time.Duration;
import java.util.Optional;

public final class LockResult<T> {

    private final LockStatus status;

    private final T value;

    private final LockHandle handle;

    private final Throwable error;

    private final String lockName;

    private final String lockKey;

    private final String ownerToken;

    private final Long fencingToken;

    private final Duration waitDuration;

    private final Duration holdDuration;

    public boolean isSuccess() {
        return status == LockStatus.SUCCESS;
    }

    public boolean isAcquired() {
        return status == LockStatus.ACQUIRED || status == LockStatus.SUCCESS;
    }

    public Optional<T> value() {
        return Optional.ofNullable(value);
    }

    public Optional<LockHandle> handle() {
        return Optional.ofNullable(handle);
    }

    // getters
}
