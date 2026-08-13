package com.xjtu.iron.transaction.provider.spring;

import com.xjtu.iron.transaction.api.TransactionParticipation;
import com.xjtu.iron.transaction.spi.ProviderTransactionContext;
import org.springframework.transaction.TransactionStatus;

import java.util.Objects;

/**
 * 把 Spring TransactionStatus 收敛成组件自己的最小 SPI 上下文。
 */
final class SpringProviderTransactionContext implements ProviderTransactionContext {

    private final TransactionStatus status;
    private final TransactionParticipation participation;

    SpringProviderTransactionContext(
            TransactionStatus status,
            TransactionParticipation participation) {
        this.status = Objects.requireNonNull(status, "status");
        this.participation = Objects.requireNonNull(participation, "participation");
    }

    @Override
    public TransactionParticipation participation() {
        return participation;
    }

    @Override
    public boolean isRollbackOnly() {
        return status.isRollbackOnly();
    }

    @Override
    public void setRollbackOnly() {
        status.setRollbackOnly();
    }
}
