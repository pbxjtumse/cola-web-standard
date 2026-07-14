package com.xjtu.iron.distributed.lock.core.runtime;

import java.util.concurrent.atomic.AtomicBoolean;
/**
 * LockHandle 的本地运行态。
 *
 * <p>
 * 该对象只描述当前 JVM 内部对一次锁租约的运行时判断，不直接等同于 Redis 中 lock key 的真实状态。
 * </p>
 *
 * <p>
 * lost 表示当前 LockHandle 已经被判断为不再持有锁，通常由 renew/check/release 返回 NOT_FOUND 或 NOT_OWNER 触发。
 * 它表示非预期失锁或释放阶段发现已经不是 owner。
 * </p>
 *
 * <p>
 * released 表示当前 LockHandle 的本地释放流程已经执行过。注意：released=true 不一定代表底层锁一定释放成功；
 * 它主要用于保证同一个 LockHandle 的 release 逻辑只执行一次。底层释放是否成功，需要结合 LockReleaseStatus 判断。
 * </p>
 */
public final class LockRuntimeState {

    /** 当前 handle 是否已经判定【非预期地丢失锁】。 */
    private final AtomicBoolean lost = new AtomicBoolean(false);

    /**
     * 当前 LockHandle 是否已经执行过本地释放流程。
     *
     * <p>
     * 注意：releaseAttempted=true 不一定表示底层锁一定释放成功。它主要用于保证同一个 LockHandle 的 release 逻辑只执行一次。
     * 底层 release 是否成功，需要结合 LockReleaseStatus 判断。
     * </p>
     */
    private final AtomicBoolean releaseAttempted = new AtomicBoolean(false);

    /**
     * 尝试把状态标记为已释放。
     *
     * @return 如果本次调用是第一次标记 releaseAttempted，返回 true；如果之前已经释放过，返回 false。
     */
    public boolean markReleasedOnce() {
        return releaseAttempted.compareAndSet(false, true);
    }

    /**
     * 尝试把状态标记为失锁。
     *
     * @return 如果本次调用是第一次标记 lost，返回 true；如果之前已经标记过，返回 false。
     */
    public boolean markLostOnce() {
        return lost.compareAndSet(false, true);
    }

    /** 获取 handle 是否已经判定失锁。 */
    public boolean isLost() { return lost.get(); }

    /** 获取 handle 是否已经判定释放锁。 */
    public boolean isReleased() { return releaseAttempted.get(); }
}
