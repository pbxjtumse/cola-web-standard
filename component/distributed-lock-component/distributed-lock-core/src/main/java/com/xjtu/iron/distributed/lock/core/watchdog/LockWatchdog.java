package com.xjtu.iron.distributed.lock.core.watchdog;

import com.xjtu.iron.distributed.lock.core.spi.LockLease;

/**
 * 锁自动续期 watchdog。定时调度、多次续期、失败标记、最大续期时间控制
 *
 * <p>watchdog 只负责任务调度和续期生命周期控制。底层“续期一次”的原子操作仍由 LockProvider.renew 完成。</p>
 */
public interface LockWatchdog {

    /**
     * 启动某个租约的自动续期。
     *
     * @param lease 锁租约。
     */
    void start(LockLease lease);

    /**
     * 停止某个租约的自动续期。
     *
     * @param lease 锁租约。
     */
    void stop(LockLease lease);
}
