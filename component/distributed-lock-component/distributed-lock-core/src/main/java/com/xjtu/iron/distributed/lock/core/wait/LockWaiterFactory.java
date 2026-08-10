package com.xjtu.iron.distributed.lock.core.wait;

import com.xjtu.iron.distributed.lock.api.LockWaitStrategy;

import java.util.Objects;

/**
 * 锁等待器工厂。
 *
 * <p>NO_WAIT/BACKOFF 由 Core 控制；PROVIDER_NATIVE 委托给具备原生 Pub/Sub waiting 能力的 Provider，
 * 当前首先由 Redisson Provider 实现。</p>
 */
public final class LockWaiterFactory {

    private final LockWaiter noWaitLockWaiter;
    private final LockWaiter backoffLockWaiter;
    private final LockWaiter providerNativeLockWaiter;

    public LockWaiterFactory() {
        this(new NoWaitLockWaiter(), new BackoffLockWaiter(), new ProviderNativeLockWaiter());
    }

    public LockWaiterFactory(
            LockWaiter noWaitLockWaiter,
            LockWaiter backoffLockWaiter,
            LockWaiter providerNativeLockWaiter
    ) {
        this.noWaitLockWaiter = Objects.requireNonNull(noWaitLockWaiter, "noWaitLockWaiter must not be null");
        this.backoffLockWaiter = Objects.requireNonNull(backoffLockWaiter, "backoffLockWaiter must not be null");
        this.providerNativeLockWaiter = Objects.requireNonNull(
                providerNativeLockWaiter, "providerNativeLockWaiter must not be null");
    }

    public LockWaiter getWaiter(LockWaitStrategy strategy) {
        Objects.requireNonNull(strategy, "strategy must not be null");
        switch (strategy) {
            case NO_WAIT:
                return noWaitLockWaiter;
            case BACKOFF:
                return backoffLockWaiter;
            case PROVIDER_NATIVE:
                return providerNativeLockWaiter;
            default:
                throw new IllegalArgumentException("unsupported wait strategy: " + strategy);
        }
    }
}
