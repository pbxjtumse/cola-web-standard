package com.xjtu.iron.distributed.lock.api.exception;

/**
 * fencing token 被业务资源拒绝。
 *
 * <p>通常表示当前执行已经是旧 owner 或过期 owner，业务 DB 条件更新返回 0，不能继续推进状态。</p>
 */
public class FencingTokenRejectedException extends DistributedLockException {

    public FencingTokenRejectedException() {
    }

    public FencingTokenRejectedException(String message) {
        super(message);
    }

    public FencingTokenRejectedException(String message, Throwable cause) {
        super(message, cause);
    }

    public FencingTokenRejectedException(Throwable cause) {
        super(cause);
    }
}
