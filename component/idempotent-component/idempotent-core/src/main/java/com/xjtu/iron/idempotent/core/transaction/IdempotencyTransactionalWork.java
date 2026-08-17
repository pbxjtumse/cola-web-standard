package com.xjtu.iron.idempotent.core.transaction;

/**
 * 幂等 Core 交给事务集成层执行的一段工作。
 *
 * <p>这里允许抛出 checked exception，是因为 {@code IdempotencyCallback} 本身允许业务代码抛出
 * {@link Exception}。具体 transaction-component 的 callback 只接受运行时异常时，由集成层负责
 * 做一次透明的 checked-exception 适配，并在事务回滚完成后恢复原始异常。</p>
 *
 * @param <T> 执行结果类型
 */
@FunctionalInterface
public interface IdempotencyTransactionalWork<T> {

    T execute() throws Exception;
}
