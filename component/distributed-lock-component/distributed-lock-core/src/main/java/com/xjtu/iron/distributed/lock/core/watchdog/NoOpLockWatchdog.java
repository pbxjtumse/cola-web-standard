package com.xjtu.iron.distributed.lock.core.watchdog;

import com.xjtu.iron.distributed.lock.api.LockOptions;

/**
 * 空 watchdog。
 */
public final class NoOpLockWatchdog implements LockWatchdog {

    @Override
    public void start(WatchdogLockHandle handle, LockOptions options) {
        // no-op
    }

    @Override
    public void stop(WatchdogLockHandle handle) {
        // no-op
    }
}
