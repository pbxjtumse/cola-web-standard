package com.xjtu.iron.distributed.lock.core.runtime;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 锁句柄运行时状态。
 *
 * <p>LockLease 是不可变租约数据；lost/released 这类会被业务线程、watchdog 线程、finally 释放逻辑同时访问的状态
 * 放在本对象中。使用 AtomicBoolean 是为了保证状态转换只发生一次。</p>
 */
public final class LockRuntimeState {

    /** 当前 handle 是否已经判定失锁。 */
    private final AtomicBoolean lost = new AtomicBoolean(false);

    /** 当前 handle 是否已经执行过释放逻辑。 */
    private final AtomicBoolean released = new AtomicBoolean(false);

    /**
     * 尝试把状态标记为已释放。
     *
     * @return 如果本次调用是第一次标记 released，返回 true；如果之前已经释放过，返回 false。
     */
    public boolean markReleasedOnce() {
        return released.compareAndSet(false, true);
    }

    /**
     * 尝试把状态标记为失锁。
     *
     * @return 如果本次调用是第一次标记 lost，返回 true；如果之前已经标记过，返回 false。
     */
    public boolean markLostOnce() {
        return lost.compareAndSet(false, true);
    }

    public boolean isLost() { return lost.get(); }
    public boolean isReleased() { return released.get(); }
}
