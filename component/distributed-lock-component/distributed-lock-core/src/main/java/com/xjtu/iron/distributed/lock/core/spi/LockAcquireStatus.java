package com.xjtu.iron.distributed.lock.core.spi;

/**
 * Provider 加锁结果状态。
 *
 * <p>这是底层 Provider 的事实结果，不是 API 最终状态。Core 层会把它映射为
 * LockStatus + LockStage。例如 NOT_ACQUIRED 通常映射为 LockStatus.NOT_ACQUIRED、LockStage.WAIT。</p>
 */
public enum LockAcquireStatus {

    /** 加锁成功。 */
    ACQUIRED,

    /** 底层锁已被其他 owner 持有，本次没有获取到锁。 */
    NOT_ACQUIRED,

    /** Provider 执行异常，例如 Redis 连接失败、Lua 执行失败或超时。 */
    PROVIDER_ERROR
}
