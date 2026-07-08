package com.xjtu.iron.distributed.lock.api;

import java.time.Duration;
import java.util.Objects;

/**
 * 分布式锁客户端。
 *
 * <p>这是业务方使用分布式锁组件的统一入口。业务代码不应该直接依赖 Redis、Zookeeper、Etcd、
 * JDBC 等底层实现，而应该通过本接口完成加锁、执行、续期、释放和持锁状态检查。</p>
 *
 * <p>本组件采用“租约模型”，不是 Java 本地锁的“线程持有模型”。一次加锁成功后会返回
 * {@link LockHandle}，其中的 {@code ownerToken} 才是证明本次锁租约归属的凭证，而不是
 * {@code Thread.currentThread()}。</p>
 *
 * <p>使用建议：业务优先使用 {@link #execute(String, LockOptions, LockCallback)}，让组件统一处理
 * 加锁、释放、异常、指标和事件；只有在确实需要跨方法或跨线程传递锁凭证时，再直接使用
 * {@link #tryLock(String, LockOptions)}。</p>
 */
public interface DistributedLockClient {

    /**
     * 尝试获取一把分布式锁。
     *
     * <p>是否等待、等待多久、租约多久、是否自动续期、是否需要 fencing token，均由
     * {@link LockOptions} 决定。</p>
     *
     * @param lockName 业务锁名称，例如 {@code settle:batch:20260708}。不能为空。
     * @param options  锁选项。不能为空。
     * @return 加锁结果。成功时 {@link LockResult#handle()} 存在；失败时返回失败状态和原因。
     */
    LockResult<LockHandle> tryLock(String lockName, LockOptions options);

    /**
     * 使用默认选项尝试获取一把分布式锁。
     *
     * <p>默认语义是：不等待、租约 30 秒、不自动续期、不要求 fencing token。</p>
     *
     * @param lockName 业务锁名称。不能为空。
     * @return 加锁结果。
     */
    default LockResult<LockHandle> tryLock(String lockName) {
        return tryLock(lockName, LockOptions.defaults());
    }

    /**
     * 在分布式锁保护下执行业务逻辑。
     *
     * <p>模板流程为：尝试加锁 -> 成功后执行业务 callback -> finally 中安全释放锁 -> 返回执行结果。
     * 该方法会把 {@link LockHandle} 传入 callback，业务可以从中获取 ownerToken、fencingToken，
     * 也可以在长流程中调用 {@link LockHandle#assertHeld()} 主动感知是否失锁。</p>
     *
     * <p>注意：分布式锁只能降低并发竞争，不替代数据库事务、业务幂等、状态机和补偿逻辑。
     * 对资金、库存、清结算等强正确性场景，业务写入仍应结合 fencing token 和 DB 条件更新。</p>
     *
     * @param lockName 业务锁名称。不能为空。
     * @param options  锁选项。不能为空。
     * @param callback 持锁后执行的业务逻辑。不能为空。
     * @param <T>      业务返回值类型。
     * @return 锁模板执行结果。
     */
    <T> LockResult<T> execute(String lockName, LockOptions options, LockCallback<T> callback);

    /**
     * 使用默认选项在分布式锁保护下执行业务逻辑。
     *
     * @param lockName 业务锁名称。不能为空。
     * @param callback 持锁后执行的业务逻辑。不能为空。
     * @param <T>      业务返回值类型。
     * @return 锁模板执行结果。
     */
    default <T> LockResult<T> execute(String lockName, LockCallback<T> callback) {
        return execute(lockName, LockOptions.defaults(), callback);
    }

    /**
     * 检查某个锁句柄是否仍然持有锁。
     *
     * <p>对 Redis Provider 来说，通常会校验 Redis 当前 value 是否仍然等于 handle 中的 ownerToken。
     * 该方法只能作为“当前时刻”的探测结果，不能保证下一毫秒仍然持有锁。</p>
     *
     * @param handle 锁句柄。不能为空。
     * @return 当前时刻仍然持有锁返回 {@code true}，否则返回 {@code false}。
     */
    boolean isHeld(LockHandle handle);

    /**
     * 安全释放锁。
     *
     * <p>释放锁必须校验 ownerToken，只有底层锁记录仍然属于本 handle 时才允许释放。
     * 这可以避免旧 owner 在锁过期后误删新 owner 的锁。</p>
     *
     * @param handle 锁句柄。不能为空。
     * @return 释放成功返回 {@code true}；锁已过期、已释放、或已不属于当前 owner 时返回 {@code false}。
     */
    boolean unlock(LockHandle handle);

    /**
     * 安全续期锁。
     *
     * <p>续期必须校验 ownerToken，只有底层锁记录仍然属于本 handle 时才允许刷新 TTL。</p>
     *
     * @param handle 锁句柄。不能为空。
     * @return 续期成功返回 {@code true}；锁已丢失或 Provider 异常时返回 {@code false}。
     */
    boolean renew(LockHandle handle);

    /**
     * 构造一个常用的短等待选项。
     *
     * @param waitTime  最长等待时间。
     * @param leaseTime 锁租约时间。
     * @return 锁选项。
     */
    static LockOptions shortWaitOptions(Duration waitTime, Duration leaseTime) {
        Objects.requireNonNull(waitTime, "waitTime must not be null");
        Objects.requireNonNull(leaseTime, "leaseTime must not be null");
        return LockOptions.builder()
                .waitTime(waitTime)
                .leaseTime(leaseTime)
                .waitStrategy(waitTime.isZero() ? LockWaitStrategy.NO_WAIT : LockWaitStrategy.BACKOFF)
                .build();
    }
}
