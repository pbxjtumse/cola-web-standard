package com.xjtu.iron.transaction.core;

import com.xjtu.iron.transaction.api.TransactionContext;
import com.xjtu.iron.transaction.api.TransactionParticipation;
import com.xjtu.iron.transaction.spi.ProviderTransactionContext;

import java.util.Objects;

/**
 * API TransactionContext 的默认桥接实现。
 */
final class DefaultTransactionContext implements TransactionContext {

    private final String executionId;
    private final String transactionName;
    private final ProviderTransactionContext providerContext;

    DefaultTransactionContext(
            String executionId,
            String transactionName,
            ProviderTransactionContext providerContext) {
        this.executionId = Objects.requireNonNull(executionId, "executionId");
        this.transactionName = Objects.requireNonNull(transactionName, "transactionName");
        this.providerContext = Objects.requireNonNull(providerContext, "providerContext");
    }

    @Override
    public String executionId() {
        return executionId;
    }

    @Override
    public String transactionName() {
        return transactionName;
    }

    @Override
    public TransactionParticipation participation() {
        return providerContext.participation();
    }

    @Override
    public boolean isRollbackOnly() {
        return providerContext.isRollbackOnly();
    }

    @Override
    public void setRollbackOnly() {
        providerContext.setRollbackOnly();
    }
}
