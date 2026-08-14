package com.xjtu.iron.transaction.api.execution;

import com.xjtu.iron.transaction.api.context.TransactionContext;

/**
 * 无返回值的事务业务回调。
 */
@FunctionalInterface
public interface TransactionRunnable {

    void execute(TransactionContext context);
}
