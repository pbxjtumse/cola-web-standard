package com.xjtu.iron.distributed.lock.core.wait;

import com.xjtu.iron.distributed.lock.core.spi.response.LockAcquireResponse;

/**
 * 不等待策略。
 */
public final class NoWaitLockWaiter implements LockWaiter {

    @Override
    public LockAcquireResponse waitForLock(LockWaitContext context) {
        return context.getProvider().acquire(context.getRequest());
    }
}
