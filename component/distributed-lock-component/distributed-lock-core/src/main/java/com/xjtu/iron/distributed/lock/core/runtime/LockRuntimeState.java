package com.xjtu.iron.distributed.lock.core.runtime;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * LockHandle 的本地运行态。
 *
 * <p>
 * 该对象只描述当前 JVM 内部对一次锁租约的运行时判断，不直接等同于 Redis、ZK 或 Etcd 中的真实锁状态。
 * </p>
 *
 * <p>
 * lost 表示当前 LockHandle 已经被组件判定为不再持有锁。典型触发来源包括 renew/check/release 返回
 * NOT_FOUND 或 NOT_OWNER。它表达的是“当前 ownerToken 已经不再是锁 owner”，通常意味着非预期失锁或
 * 释放阶段发现锁已经不属于当前 handle。
 * </p>
 *
 * <p>
 * releaseAttempted 表示当前 LockHandle 已经进入过本地释放流程。注意：releaseAttempted=true 不代表底层锁
 * 一定释放成功；它只用于保证同一个 LockHandle 的 release 逻辑只真正执行一次。底层释放结果必须结合
 * LockReleaseStatus 或 LockResult 判断。
 * </p>
 */
public final class LockRuntimeState {

    /** 当前 handle 是否已经被判定失锁。 */
    private final AtomicBoolean lost = new AtomicBoolean(false);

    /** 当前 handle 是否已经进入过本地释放流程。 */
    private final AtomicBoolean releaseAttempted = new AtomicBoolean(false);

    /**
     * 尝试把当前 handle 标记为已经进入释放流程。
     *
     * @return 如果本次调用是第一次进入释放流程，返回 true；否则返回 false。
     */
    public boolean markReleaseAttemptedOnce() {
        return releaseAttempted.compareAndSet(false, true);
    }


    /**
     * 尝试把当前 handle 标记为失锁。
     *
     * @return 如果本次调用是第一次标记 lost，返回 true；否则返回 false。
     */
    public boolean markLostOnce() {
        return lost.compareAndSet(false, true);
    }

    /**
     * 当前 handle 是否已经被判定失锁。
     *
     * @return 已判定失锁返回 true。
     */
    public boolean isLost() {
        return lost.get();
    }

    /**
     * 当前 handle 是否已经进入过本地释放流程。
     *
     * @return 已进入过本地释放流程返回 true。
     */
    public boolean isReleaseAttempted() {
        return releaseAttempted.get();
    }

}
