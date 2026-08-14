package com.xjtu.iron.transaction.spi.provider;

/**
 * Provider 暴露给 core 的最小事务运行时能力。
 *
 * <p>Spring TransactionStatus 等底层框架类型不得泄漏到 transaction-api。</p>
 */
public interface ProviderTransactionContext {

    boolean isRollbackOnly();

    void setRollbackOnly();
}
