package com.xjtu.iron.distributed.lock.api;

/**
 * 有返回值的持锁业务回调。
 *
 * <p>{@link DistributedLockClient#execute(String, LockOptions, LockCallback)} 在成功获取锁之后会执行本回调，
 * 并把本次加锁成功产生的 {@link LockHandle} 传入业务方。</p>
 *
 * <p>回调里可以读取 {@link LockHandle#ownerToken()} 作为日志追踪信息，也可以读取
 * {@link LockHandle#fencingToken()} 作为业务资源写入的版本保护。长耗时业务可以在关键写入前调用
 * {@link LockHandle#assertHeld()} 提前发现失锁，但最终正确性仍应依赖 DB 条件更新和 fencing token。</p>
 *
 * @param <T> 业务返回值类型。
 */
@FunctionalInterface
public interface LockCallback<T> {

    /**
     * 持锁执行业务逻辑。
     *
     * @param handle 本次加锁成功后的锁句柄。不能为空。
     * @return 业务返回值。
     * @throws Exception 业务执行异常。模板层会捕获并转换为 {@link LockResult}。
     */
    T doWithLock(LockHandle handle) throws Exception;
}
