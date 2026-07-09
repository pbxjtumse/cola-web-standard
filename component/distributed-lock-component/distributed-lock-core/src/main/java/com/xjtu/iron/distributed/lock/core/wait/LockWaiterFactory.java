package com.xjtu.iron.distributed.lock.core.wait;

import com.xjtu.iron.distributed.lock.api.LockWaitStrategy;

/**
 * 锁等待器工厂。
 */
public final class LockWaiterFactory {

    private final LockWaiter noWaitLockWaiter;
    private final LockWaiter backoffLockWaiter;

    public LockWaiterFactory() {
        this(new NoWaitLockWaiter(), new BackoffLockWaiter());
    }

    public LockWaiterFactory(LockWaiter noWaitLockWaiter, LockWaiter backoffLockWaiter) {
        this.noWaitLockWaiter = noWaitLockWaiter;
        this.backoffLockWaiter = backoffLockWaiter;
    }

    public LockWaiter getWaiter(LockWaitStrategy strategy) {
        if (strategy == LockWaitStrategy.NO_WAIT) {
            return noWaitLockWaiter;
        }
        if (strategy == LockWaitStrategy.BACKOFF) {
            return backoffLockWaiter;
        }
        throw new UnsupportedOperationException("wait strategy not implemented yet: " + strategy);
    }
}
