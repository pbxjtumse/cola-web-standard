package com.xjtu.iron.distributed.lock.api.exception;

/**
 * 分布式锁组件基础异常。
 */
public class DistributedLockException extends RuntimeException {

    public DistributedLockException() {
        super();
    }

    public DistributedLockException(String message) {
        super(message);
    }

    public DistributedLockException(String message, Throwable cause) {
        super(message, cause);
    }

    public DistributedLockException(Throwable cause) {
        super(cause);
    }
}
