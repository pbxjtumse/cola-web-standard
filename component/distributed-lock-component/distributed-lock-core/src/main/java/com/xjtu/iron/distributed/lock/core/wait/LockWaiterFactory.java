package com.xjtu.iron.distributed.lock.core.wait;

import com.xjtu.iron.distributed.lock.api.LockWaitStrategy;

import java.util.Objects;

/**
 * 锁等待器工厂。
 *
 * <p>一期只支持 {@link LockWaitStrategy#NO_WAIT} 和 {@link LockWaitStrategy#BACKOFF}。
 * Pub/Sub 等复杂等待策略不在一期暴露，避免 API 能力与实现能力不一致。</p>
 */
public final class LockWaiterFactory {

    /** 不等待实现。 */
    private final LockWaiter noWaitLockWaiter;

    /** 退避等待实现。 */
    private final LockWaiter backoffLockWaiter;

    public LockWaiterFactory() {
        this(new NoWaitLockWaiter(), new BackoffLockWaiter());
    }

    public LockWaiterFactory(LockWaiter noWaitLockWaiter, LockWaiter backoffLockWaiter) {
        this.noWaitLockWaiter = Objects.requireNonNull(noWaitLockWaiter, "noWaitLockWaiter must not be null");
        this.backoffLockWaiter = Objects.requireNonNull(backoffLockWaiter, "backoffLockWaiter must not be null");
    }

    /**
     * 根据等待策略选择等待器。
     *
     * @param strategy 等待策略。
     * @return 对应等待器。
     */
    public LockWaiter getWaiter(LockWaitStrategy strategy) {
        Objects.requireNonNull(strategy, "strategy must not be null");
        switch (strategy) {
            case NO_WAIT:
                return noWaitLockWaiter;
            case BACKOFF:
                return backoffLockWaiter;
            default:
                throw new IllegalArgumentException("unsupported wait strategy: " + strategy);
        }
    }
}
