package com.xjtu.iron.distributed.lock.api.exception;

/**
 * 当前 ownerToken 不是锁 owner。
 *
 * <p>常见于 unlock 或 renew 时发现底层 value 已经不是当前 handle 的 ownerToken。</p>
 */
public class LockNotOwnerException extends DistributedLockException {

    public LockNotOwnerException() {
    }

    public LockNotOwnerException(String message) {
        super(message);
    }

    public LockNotOwnerException(String message, Throwable cause) {
        super(message, cause);
    }

    public LockNotOwnerException(Throwable cause) {
        super(cause);
    }
}
