package com.xjtu.iron.distributed.lock.api.exception;

/**
 * 锁选项非法。
 */
public class InvalidLockOptionsException extends DistributedLockException {

    public InvalidLockOptionsException() {
    }

    public InvalidLockOptionsException(String message) {
        super(message);
    }

    public InvalidLockOptionsException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidLockOptionsException(Throwable cause) {
        super(cause);
    }
}
