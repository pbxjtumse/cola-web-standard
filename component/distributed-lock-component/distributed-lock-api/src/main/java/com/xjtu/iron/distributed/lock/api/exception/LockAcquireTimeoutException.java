package com.xjtu.iron.distributed.lock.api.exception;

/**
 * 在指定等待时间内没有获取到锁。
 */
public class LockAcquireTimeoutException extends DistributedLockException {

    public LockAcquireTimeoutException() {
    }

    public LockAcquireTimeoutException(String message) {
        super(message);
    }

    public LockAcquireTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }

    public LockAcquireTimeoutException(Throwable cause) {
        super(cause);
    }
}
