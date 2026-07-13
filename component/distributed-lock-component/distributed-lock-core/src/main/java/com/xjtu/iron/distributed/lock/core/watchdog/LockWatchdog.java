package com.xjtu.iron.distributed.lock.core.watchdog;

import com.xjtu.iron.distributed.lock.api.LockOptions;

/**
 * 锁自动续期 watchdog。
 *
 * <p>watchdog 只负责调度和续期生命周期控制。底层“续期一次”的原子操作仍由 LockHandle.renew() 间接调用
 * LockProvider.renew() 完成。</p>
 */
public interface LockWatchdog {

    /**
     * 启动某个锁句柄的自动续期。
     *
     * @param handle  支持 watchdog 操作的锁句柄。
     * @param options 本次加锁选项，用于读取 renewInterval、maxRenewTime 等配置。
     */
    void start(WatchdogLockHandle handle, LockOptions options);

    /**
     * 停止某个锁句柄的自动续期。
     *
     * @param handle 支持 watchdog 操作的锁句柄。
     */
    void stop(WatchdogLockHandle handle);
}
