package com.xjtu.iron.distributed.lock.core.watchdog;

import com.xjtu.iron.distributed.lock.core.spi.LockLease;

/**
 * watchdog 注册表。
 */
public interface LockWatchdogRegistry {

    void register(LockLease lease);

    void unregister(LockLease lease);

    boolean contains(LockLease lease);
}
