package com.xjtu.iron.distributed.lock.api;

import java.util.Objects;

/**
 * 分布式锁客户端。负责创建锁租约，提供模板执行入口
 * <p>这是业务方使用分布式锁组件的统一入口。业务代码不应该直接依赖 Redis、Zookeeper、Etcd、JDBC
 * 等底层实现，而应该通过本接口完成加锁、执行、续期、释放和持锁状态检查。</p>
 *
 * <p>本组件采用“租约模型”，不是 Java 本地锁的“线程持有模型”。一次加锁成功后会返回
 * {@link LockHandle}，其中的 {@code ownerToken} 才是证明本次锁租约归属的凭证，组件不会把
 * {@code Thread.currentThread()} 作为分布式锁 owner。</p>
 *
 * <p>使用建议：
 * <ol>
 *     <li>业务优先使用 {@link #execute(String, LockOptions, LockCallback)}  </li>
 *     <li>{@link #execute(String, LockOptions, LockRunnable)}，让组件统一处理加锁、释放、异常、指标和事件； </li>
 *     <li>只有在确实需要跨方法或跨线程传递锁凭证时，再直接使用 {@link #tryLock(String, LockOptions)}。</p></li>
 * </ol>
 */
public interface DistributedLockClient {

    /**
     * tryLock 是低层 API 根据用户设置的 options 尝试获取一把分布式锁。
     *
     * <p>
     * {@code tryLock} 是底层加锁 API。获取成功后，需要由业务方自行负责：
     * </p>
     *
     * <ol>
     *     <li>判断是否成功获取锁；</li>
     *     <li>执行业务逻辑；</li>
     *     <li>捕获业务异常；</li>
     *     <li>在 {@code finally} 中释放锁；</li>
     *     <li>处理锁释放失败；</li>
     *     <li>处理锁租约丢失，即 {@code LockLost}；</li>
     *     <li>处理 fencing token 被业务系统拒绝的情况。</li>
     * </ol>
     * <p>基本使用示例：</p>
     *
     * <pre>{@code
     * LockResult<LockHandle> result =
     *         lockClient.tryLock("batch:001", options);
     *
     * if (!result.isAcquired()) {
     *     handleAcquireFailure(result);
     *     return;
     * }
     *
     * LockHandle handle = result.handle().orElseThrow();
     *
     * try {
     *     doBusiness(handle.fencingToken());
     * } catch (Exception ex) {
     *     throw ex;
     * } finally {
     *     try {
     *         handle.unlock();
     *     } catch (Exception releaseException) {
     *         handleReleaseFailure(releaseException);
     *     }
     * }
     * }</pre>
     *
     * @param lockName 业务锁名称，例如 {@code settle:batch:20260708}，不能为空
     * @param options  锁选项，不能为空
     * @return 加锁结果；成功时 {@link LockResult#handle()} 存在，
     *         失败时返回对应的失败状态和原因
     */
    LockResult<LockHandle> tryLock(String lockName, LockOptions options);

    /**
     * 使用默认选项尝试获取一把分布式锁。
     *
     * <p>默认语义是：不等待、租约 30 秒、不自动续期、不要求 fencing token、使用默认 Provider、使用默认 namespace </p>
     *
     * @param lockName 业务锁名称。不能为空。
     * @return 加锁结果。
     */
    default LockResult<LockHandle> tryLock(String lockName) {
        return tryLock(lockName, LockOptions.defaults());
    }

    /**
     * execute 是模板 API 在分布式锁保护下执行业务逻辑，并返回业务结果。
     *
     * <p>模板流程为：尝试加锁 -> 成功后执行业务 callback -> finally 中安全释放锁 -> 返回执行结果。
     * 该方法会把 {@link LockHandle} 传入 callback，业务可以从中获取 ownerToken、fencingToken，
     * 也可以在长流程中调用 {@link LockHandle#assertHeld()} 主动感知是否失锁。</p>
     *
     * <ol>
     *     <li>参数校验；</li>
     *     <li>获取锁；</li>
     *     <li>获取不到锁时返回 {@code NOT_ACQUIRED}；</li>
     *     <li>创建 {@link LockHandle}；</li>
     *     <li>启动 watchdog；</li>
     *     <li>执行业务 callback；</li>
     *     <li>捕获业务异常；</li>
     *     <li>捕获 {@link LockLostException}；</li>
     *     <li>捕获 {@link FencingTokenRejectedException}；</li>
     *     <li>在 {@code finally} 中执行 {@code unlock}；</li>
     *     <li>停止 watchdog；</li>
     *     <li>生成 {@link LockResult}；</li>
     *     <li>发布事件；</li>
     *     <li>记录指标。</li>
     * </ol>
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
     * 使用默认选项在分布式锁保护下执行业务逻辑，并返回业务结果。
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
     * 在分布式锁保护下执行无返回值业务逻辑。
     *
     * <p>这是 {@link #execute(String, LockOptions, LockCallback)} 的便捷版本，避免业务方为了无返回值场景
     * 手写 {@code return null}。</p>
     *
     * @param lockName 业务锁名称。不能为空。
     * @param options  锁选项。不能为空。
     * @param runnable 持锁后执行的业务逻辑。不能为空。
     * @return 锁模板执行结果。
     */
    default LockResult<Void> execute(String lockName, LockOptions options, LockRunnable runnable) {
        Objects.requireNonNull(runnable, "runnable must not be null");
        return execute(lockName, options, handle -> {
            runnable.runWithLock(handle);
            return null;
        });
    }

    /**
     * 使用默认选项在分布式锁保护下执行无返回值业务逻辑。
     *
     * @param lockName 业务锁名称。不能为空。
     * @param runnable 持锁后执行的业务逻辑。不能为空。
     * @return 锁模板执行结果。
     */
    default LockResult<Void> execute(String lockName, LockRunnable runnable) {
        return execute(lockName, LockOptions.defaults(), runnable);
    }

}
