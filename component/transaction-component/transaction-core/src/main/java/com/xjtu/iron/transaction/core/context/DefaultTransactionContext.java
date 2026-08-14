package com.xjtu.iron.transaction.core.context;

import com.xjtu.iron.transaction.api.context.TransactionContext;
import com.xjtu.iron.transaction.api.context.TransactionParticipation;
import com.xjtu.iron.transaction.spi.provider.ProviderTransactionContext;

import java.util.Objects;

/**
 * API TransactionContext 与 ProviderTransactionContext 之间的默认桥接实现。
 */
public final class DefaultTransactionContext implements TransactionContext {

    private final String executionId;
    private final String transactionName;
    private final ProviderTransactionContext providerContext;

    public DefaultTransactionContext(
            String executionId,
            String transactionName,
            ProviderTransactionContext providerContext) {
        this.executionId = Objects.requireNonNull(executionId, "executionId");
        this.transactionName = Objects.requireNonNull(transactionName, "transactionName");
        this.providerContext = Objects.requireNonNull(providerContext, "providerContext");
    }

    @Override
    public String executionId() { return executionId; }

    @Override
    public String transactionName() { return transactionName; }

    @Override
    public TransactionParticipation participation() {
        // participation 的真实来源是底层事务管理器，core 不自行猜测当前是否创建新事务。
        return providerContext.participation();
    }

    @Override
    public boolean isRollbackOnly() {
        // 实时读取 Provider 状态，确保外层事务已被标记 rollback-only 时调用方能够感知。
        return providerContext.isRollbackOnly();
    }

    @Override
    public void setRollbackOnly() {
        // 只通过 SPI 标记 rollback-only，不把 Spring TransactionStatus 泄漏给业务层。
        providerContext.setRollbackOnly();
    }
}
