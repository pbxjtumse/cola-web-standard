package com.xjtu.iron.distributed.lock.api.exception;

/**
 * 锁已经丢失。
 *
 * <p>例如租约过期、续期失败、底层 ownerToken 已不匹配。业务捕获该异常后不应该继续推进核心状态。</p>
 */
public class LockLostException extends DistributedLockException {

    public LockLostException() {
    }

    public LockLostException(String message) {
        super(message);
    }

    public LockLostException(String message, Throwable cause) {
        super(message, cause);
    }

    public LockLostException(Throwable cause) {
        super(cause);
    }
}
