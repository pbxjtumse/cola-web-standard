package com.xjtu.iron.distributed.lock.core.spi;


import com.xjtu.iron.distributed.lock.api.LockOptions;

public final class LockAcquireRequest {

    private final String lockName;

    private final String lockKey;

    private final String ownerToken;

    private final LockOptions options;

    // getters
}
