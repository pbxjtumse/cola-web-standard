package com.xjtu.iron.distributed.lock.core.watchdog;

import com.xjtu.iron.distributed.lock.core.spi.LockLease;

/**
 * 空 watchdog。
 */
public final class NoOpLockWatchdog implements LockWatchdog {

    @Override public void start(LockLease lease) { }
    @Override public void stop(LockLease lease) { }
}
