package com.xjtu.iron.transaction.spi;

/**
 * Provider 在真正进入事务边界以后调用的业务回调。
 */
@FunctionalInterface
public interface TransactionProviderCallback<T> {

    T execute(ProviderTransactionContext context);
}
