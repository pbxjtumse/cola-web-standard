package com.xjtu.iron.distributed.lock.core.event;

/**
 * 分布式锁事件类型。
 *
 * <p>
 * 事件类型用于描述发生了什么事情。
 * LockStage 用于描述事情发生在哪个阶段。
 * LockStatus 用于描述最终或当前状态。
 * </p>
 */
public enum LockEventType {

    /**
     * 开始尝试获取锁。
     */
    ACQUIRE_ATTEMPT,

    /**
     * 获取锁成功。
     */
    ACQUIRED,

    /**
     * 没有获取到锁。
     */
    NOT_ACQUIRED,

    /**
     * 业务开始执行。
     */
    EXECUTION_STARTED,

    /**
     * 业务执行成功。
     */
    EXECUTION_SUCCESS,

    /**
     * 业务执行失败。
     */
    EXECUTION_FAILED,

    /**
     * 续期成功。
     */
    RENEWED,

    /**
     * 续期失败。
     */
    RENEW_FAILED,

    /**
     * 锁丢失。
     */
    LOCK_LOST,

    /**
     * 释放成功。
     */
    RELEASED,

    /**
     * 释放失败。
     */
    RELEASE_FAILED,

    /**
     * fencing token 被业务资源拒绝。
     */
    FENCING_REJECTED,

    /**
     * Provider 异常。
     */
    PROVIDER_ERROR
}