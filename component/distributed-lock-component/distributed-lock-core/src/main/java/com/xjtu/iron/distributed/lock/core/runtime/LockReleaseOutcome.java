package com.xjtu.iron.distributed.lock.core.runtime;

import com.xjtu.iron.distributed.lock.core.spi.response.LockReleaseResponse;
import com.xjtu.iron.distributed.lock.core.spi.status.LockReleaseStatus;

/**
 * LockHandle 本地释放流程的结果。
 *
 * <p>
 * 该对象属于 core 内部语义，不是 Provider SPI 响应。它用于区分：
 * </p>
 *
 * <ul>
 *     <li>本次确实调用 Provider 并释放成功；</li>
 *     <li>本次调用 Provider 时发现锁已经丢失；</li>
 *     <li>本次调用 Provider 时发生 Provider 异常；</li>
 *     <li>本地释放流程之前已经执行过，本次只是重复 unlock/close/finally。</li>
 * </ul>
 *
 * <p>
 * 不要用 {@link LockReleaseResponse} 表达本地重复释放，因为重复释放不是 Redis key 不存在，
 * 也不是 ownerToken 不匹配。ProviderStatus 只表达底层 Provider 的事实，不能混入本地幂等控制语义。
 * </p>
 */
public final class LockReleaseOutcome {

    /** 本地释放流程结果类型。 */
    public enum Type {
        /** 本次调用 Provider，底层锁释放成功。 */
        RELEASED,

        /** 本次调用 Provider，发现锁不存在或 ownerToken 不匹配。 */
        LOCK_LOST,

        /** 本次调用 Provider，底层 Provider 报错。 */
        RELEASE_FAILED,

        /** 本地释放流程之前已经执行过，本次没有再次调用 Provider。 */
        ALREADY_ATTEMPTED
    }

    private final Type type;

    private final LockReleaseStatus providerStatus;

    private final Throwable error;

    private LockReleaseOutcome(Type type, LockReleaseStatus providerStatus, Throwable error) {
        if (type == null) {
            throw new IllegalArgumentException("type must not be null");
        }
        this.type = type;
        this.providerStatus = providerStatus;
        this.error = error;
    }

    public static LockReleaseOutcome released() {
        return new LockReleaseOutcome(Type.RELEASED, LockReleaseStatus.RELEASED, null);
    }

    public static LockReleaseOutcome lockLost(LockReleaseStatus providerStatus) {
        if (providerStatus != LockReleaseStatus.NOT_FOUND && providerStatus != LockReleaseStatus.NOT_OWNER) {
            throw new IllegalArgumentException("providerStatus must be NOT_FOUND or NOT_OWNER when lock lost");
        }
        return new LockReleaseOutcome(Type.LOCK_LOST, providerStatus, null);
    }

    public static LockReleaseOutcome releaseFailed(Throwable error) {
        return new LockReleaseOutcome(Type.RELEASE_FAILED, LockReleaseStatus.PROVIDER_ERROR, error);
    }

    public static LockReleaseOutcome alreadyAttempted() {
        return new LockReleaseOutcome(Type.ALREADY_ATTEMPTED, null, null);
    }

    public Type type() {
        return type;
    }

    public Type getType() {
        return type;
    }

    public LockReleaseStatus providerStatus() {
        return providerStatus;
    }

    public LockReleaseStatus getProviderStatus() {
        return providerStatus;
    }

    public Throwable error() {
        return error;
    }

    public Throwable getError() {
        return error;
    }

    public boolean isReleased() {
        return type == Type.RELEASED;
    }

    public boolean isLockLost() {
        return type == Type.LOCK_LOST;
    }

    public boolean isReleaseFailed() {
        return type == Type.RELEASE_FAILED;
    }

    public boolean isAlreadyAttempted() {
        return type == Type.ALREADY_ATTEMPTED;
    }
}
