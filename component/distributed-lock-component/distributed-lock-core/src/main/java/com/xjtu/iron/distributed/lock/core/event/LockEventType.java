package com.xjtu.iron.distributed.lock.core.event;

/**
 * 分布式锁事件类型。
 */
public enum LockEventType {
    ACQUIRE_ATTEMPT,
    ACQUIRED,
    ACQUIRE_FAILED,
    WAIT_RETRY,
    RENEW_SUCCESS,
    RENEW_FAILED,
    LOCK_LOST,
    RELEASED,
    RELEASE_FAILED,
    EXECUTION_SUCCESS,
    EXECUTION_FAILED,
    FENCING_REJECTED
}
