package com.xjtu.iron.transaction.spi;

import com.xjtu.iron.transaction.api.TransactionParticipation;

/**
 * Provider 暴露给 core 的最小事务运行时能力。
 *
 * <p>禁止把 Spring TransactionStatus 等 Provider 专属类型泄漏到 transaction-api。</p>
 */
public interface ProviderTransactionContext {

    TransactionParticipation participation();

    boolean isRollbackOnly();

    void setRollbackOnly();
}
