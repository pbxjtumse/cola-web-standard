package com.xjtu.iron.transaction.api;

/**
 * 有返回值的事务业务回调。
 *
 * <p>一期只允许回调直接抛出 RuntimeException / Error。
 * 事务组件不会吞掉业务异常；业务异常会触发回滚，然后保持原异常继续向上抛出。</p>
 */
@FunctionalInterface
public interface TransactionCallback<T> {

    T execute(TransactionContext context);
}
