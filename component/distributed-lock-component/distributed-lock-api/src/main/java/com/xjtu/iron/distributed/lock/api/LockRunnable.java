package com.xjtu.iron.distributed.lock.api;

/**
 * 无返回值的持锁业务回调。
 *
 * <p>该接口用于 {@link DistributedLockClient#execute(String, LockOptions, LockRunnable)}，适合只需要在锁保护下
 * 执行操作、不需要返回业务结果的场景。</p>
 */
@FunctionalInterface
public interface LockRunnable {

    /**
     * 持锁执行业务逻辑。
     *
     * @param handle 本次加锁成功后的锁句柄。不能为空。
     * @throws Exception 业务执行异常。模板层会捕获并转换为 {@link LockResult}。
     */
    void runWithLock(LockHandle handle) throws Exception;
}
