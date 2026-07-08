package com.xjtu.iron.distributed.lock.api;


public enum LockStatus {

    /**
     * 加锁成功，仅用于 tryLock。
     */
    ACQUIRED,

    /**
     * 执行成功，用于 execute。
     */
    SUCCESS,

    /**
     * 在 waitTime 内没有拿到锁。
     */
    NOT_ACQUIRED,

    /**
     * 业务执行失败。
     */
    EXECUTION_FAILED,

    /**
     * 执行过程中锁丢失。
     */
    LOCK_LOST,

    /**
     * fencing token 被业务资源拒绝。
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
     * 参数非法。
     */
    INVALID_OPTIONS
}
