package com.xjtu.iron.distributed.lock.core.wait;

import com.xjtu.iron.distributed.lock.core.spi.LockAcquireResponse;

/**
 * 锁等待器。
 *
 * <p>LockWaiter 负责“第一次没有抢到锁之后怎么等”。它不负责创建 ownerToken，
 * 不负责构造 LockHandle，也不负责执行 callback。</p>
 */
public interface LockWaiter {

    /**
     * 在指定等待上下文中尝试获取锁。
     *
     * @param context 等待上下文，包含 request、provider、clock 等信息。
     * @return Provider 加锁响应。
     */
    LockAcquireResponse waitForLock(LockWaitContext context);
}
