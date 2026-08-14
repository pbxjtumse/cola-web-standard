package com.xjtu.iron.transaction.api.execution;

import com.xjtu.iron.transaction.api.context.TransactionContext;

/**
 * 有返回值的事务业务回调。
 *
 * <p>业务异常不会被事务组件吞掉；Provider 完成回滚处理后，原异常继续向上抛出。</p>
 *
 * @param <T> 业务返回值类型
 */
@FunctionalInterface
public interface TransactionCallback<T> {

    T execute(TransactionContext context);
}
