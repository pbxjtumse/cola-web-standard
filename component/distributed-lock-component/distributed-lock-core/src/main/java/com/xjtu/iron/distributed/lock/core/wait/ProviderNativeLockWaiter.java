package com.xjtu.iron.distributed.lock.core.wait;

import com.xjtu.iron.distributed.lock.core.spi.protocol.LockAcquireResponse;

/**
 * Provider 原生等待器。
 *
 * <p>该等待器自己不实现 Pub/Sub，而是只调用 Provider.acquire() 一次，并把完整 waitTime 交给 Provider。
 * Redisson Provider 会使用 RLock/RFencedLock 的原生 Pub/Sub 等待机制。这样 Core 不需要理解 Redisson
 * 的订阅 channel、唤醒、超时和重连细节。</p>
 */
public final class ProviderNativeLockWaiter implements LockWaiter {

    @Override
    public LockAcquireResponse waitForLock(LockWaitContext context) {
        return context.getProvider().acquire(context.getRequest());
    }
}
