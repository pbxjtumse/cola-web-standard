package com.xjtu.iron.distributed.lock.api;

/**
 * 分布式锁操作最终状态。
 *
 * <p>LockStatus 只描述一次 {@code tryLock} 或 {@code execute} 的最终结果。
 * 错误发生在哪个生命周期阶段，应通过 {@link LockResult#stage()} 查看。</p>
 *
 * <p>注意：是否曾经成功获取过锁不能只通过本枚举推断，应使用
 * {@link LockResult#isAcquired()} 或 {@link LockResult#acquired()} 判断。</p>
 */
public enum LockStatus {

    /** 加锁成功，仅用于 tryLock。 */
    ACQUIRED,

    /** 加锁并执行业务成功，用于 execute。 */
    SUCCESS,

    /**
     * 在 waitTime 内没有获取到锁。
     *
     * <p>这是正常竞争失败，不代表 Redis、ZK、Etcd 或组件自身异常。</p>
     */
    NOT_ACQUIRED,

    /**
     * 业务回调执行失败。
     */
    EXECUTION_FAILED,

    /**
     * 锁已经丢失。
     *
     * <p>例如续期时发现 key 不存在、ownerToken 不匹配，或者 checkHeld / assertHeld
     * 发现当前请求已经不是 owner。</p>
     */
    LOCK_LOST,

    /**
     * fencing token 被业务资源拒绝。
     *
     * <p>通常表示当前请求已经不是最新 owner，或者业务状态已经被其他请求推进。</p>
     */
    FENCING_REJECTED,

    /**
     * 释放阶段发生不可忽略的失败。
     */
    RELEASE_FAILED,

    /**
     * 底层 Provider 异常。
     *
     * <p>例如 Redis 连接失败、Lua 执行失败、Redis 超时、ZK/Etcd 客户端异常。</p>
     */
    PROVIDER_ERROR,

    /**
     * 参数或锁选项非法。
     */
    INVALID_OPTIONS
}
