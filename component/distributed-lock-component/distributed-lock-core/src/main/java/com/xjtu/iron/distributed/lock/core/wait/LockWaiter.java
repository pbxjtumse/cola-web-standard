package com.xjtu.iron.distributed.lock.core.wait;

import com.xjtu.iron.distributed.lock.core.spi.LockAcquireResponse;

/**
 * 锁等待器。
 *
 * <p>不同等待策略实现本接口，例如 NO_WAIT 尝试一次，BACKOFF 在 waitTime 内退避重试，PUBSUB_BACKOFF 等待释放通知
 * 并配合本地退避兜底。</p>
 */
public interface LockWaiter {

    /**
     * 等待并尝试获取锁。
     *
     * @param context 等待上下文。
     * @return 加锁响应。
     */
    LockAcquireResponse waitForLock(LockWaitContext context);
}
