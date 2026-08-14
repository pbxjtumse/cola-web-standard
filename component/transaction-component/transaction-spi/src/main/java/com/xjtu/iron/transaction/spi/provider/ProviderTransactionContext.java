package com.xjtu.iron.transaction.spi.provider;

import com.xjtu.iron.transaction.api.context.TransactionParticipation;

/**
 * Provider 暴露给 core 的最小事务运行时能力。
 *
 * <p>Spring TransactionStatus 等具体框架类型不得泄漏到 transaction-api。</p>
 */
public interface ProviderTransactionContext {

    TransactionParticipation participation();

    boolean isRollbackOnly();

    void setRollbackOnly();
}
