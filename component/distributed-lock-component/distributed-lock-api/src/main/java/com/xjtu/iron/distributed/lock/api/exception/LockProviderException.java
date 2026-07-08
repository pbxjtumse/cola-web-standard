package com.xjtu.iron.distributed.lock.api.exception;

/**
 * 底层锁 Provider 异常。
 */
public class LockProviderException extends DistributedLockException {

    public LockProviderException() {
    }

    public LockProviderException(String message) {
        super(message);
    }

    public LockProviderException(String message, Throwable cause) {
        super(message, cause);
    }

    public LockProviderException(Throwable cause) {
        super(cause);
    }
}
