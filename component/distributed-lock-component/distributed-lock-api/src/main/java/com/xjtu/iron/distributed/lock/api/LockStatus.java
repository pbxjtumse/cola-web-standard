package com.xjtu.iron.distributed.lock.api;

/**
 * 锁操作结果状态。
 */
public enum LockStatus {

    /**
     * 加锁成功。
     *
     * <p>主要用于 {@link DistributedLockClient#tryLock(String, LockOptions)} 返回。</p>
     */
    ACQUIRED,

    /**
     * 持锁业务执行成功。
     *
     * <p>主要用于 {@link DistributedLockClient#execute(String, LockOptions, LockCallback)} 返回。</p>
     */
    SUCCESS,

    /**
     * 在 waitTime 内没有获取到锁。
     */
    NOT_ACQUIRED,

    /**
     * 持锁后的业务 callback 执行失败。
     */
    EXECUTION_FAILED,

    /**
     * 执行过程中锁丢失。
     *
     * <p>例如 watchdog 续期失败、主动 isHeld 检查失败、释放时发现 ownerToken 不匹配。</p>
     */
    LOCK_LOST,

    /**
     * fencing token 被业务资源拒绝。
     *
     * <p>通常表示旧 owner 恢复后尝试写入，DB 条件更新返回 0，业务抛出 FencingTokenRejectedException。</p>
     */
    FENCING_REJECTED,

    /**
     * 解锁失败。
     */
    RELEASE_FAILED,

    /**
     * 续期失败。
     */
    RENEW_FAILED,

    /**
     * 底层 Provider 异常。
     */
    PROVIDER_ERROR,

    /**
     * 锁参数非法。
     */
    INVALID_OPTIONS
}
