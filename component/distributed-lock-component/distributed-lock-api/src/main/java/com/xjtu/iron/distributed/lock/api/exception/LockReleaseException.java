package com.xjtu.iron.distributed.lock.api.exception;

/**
 * 解锁失败异常。
 */
public class LockReleaseException extends DistributedLockException {

    public LockReleaseException() {
    }

    public LockReleaseException(String message) {
        super(message);
    }

    public LockReleaseException(String message, Throwable cause) {
        super(message, cause);
    }

    public LockReleaseException(Throwable cause) {
        super(cause);
    }
}
