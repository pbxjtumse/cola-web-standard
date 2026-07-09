package com.xjtu.iron.distributed.lock.api;

/**
 * 分布式锁操作状态。，表示最终结果。
 *
 * <p>该枚举描述一次 tryLock 或 execute 的最终结果。是否曾经成功获取锁不应该只通过该状态推断，
 * 应使用 {@link LockResult#isAcquired()} 判断。</p>
 */
public enum LockStatus {

    /** 加锁成功，仅用于 tryLock。 */
    ACQUIRED,

    /** 加锁并执行业务成功，用于 execute。 */
    SUCCESS,

    /** Redis 正常 在 waitTime 内没有获取到锁，属于正常竞争失败。 */
    NOT_ACQUIRED,

    /** 持锁业务执行失败。 */
    EXECUTION_FAILED,

    /** 执行过程中锁丢失，例如续期失败或 ownerToken 不匹配。 */
    LOCK_LOST,

    /** fencing token 被业务资源拒绝，通常表示当前 owner 已经过期或状态被更新。 */
    FENCING_REJECTED,

    /** 解锁失败。 */
    RELEASE_FAILED,

    /** 底层 Provider 执行异常包括 Redis 连接失败， Redis Lua 执行失败， Redis 超时， Provider 内部异常  */
    PROVIDER_ERROR,

    /** 锁选项或参数非法。 */
    INVALID_OPTIONS
}
