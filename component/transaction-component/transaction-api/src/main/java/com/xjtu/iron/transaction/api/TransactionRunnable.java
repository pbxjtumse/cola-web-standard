package com.xjtu.iron.transaction.api;

/**
 * 无返回值的事务业务回调。
 */
@FunctionalInterface
public interface TransactionRunnable {

    void execute(TransactionContext context);
}
