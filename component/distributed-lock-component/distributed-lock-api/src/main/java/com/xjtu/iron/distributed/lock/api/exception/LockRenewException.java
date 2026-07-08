package com.xjtu.iron.distributed.lock.api.exception;

/**
 * 续期失败异常。
 */
public class LockRenewException extends DistributedLockException {

    public LockRenewException() {
    }

    public LockRenewException(String message) {
        super(message);
    }

    public LockRenewException(String message, Throwable cause) {
        super(message, cause);
    }

    public LockRenewException(Throwable cause) {
        super(cause);
    }
}
