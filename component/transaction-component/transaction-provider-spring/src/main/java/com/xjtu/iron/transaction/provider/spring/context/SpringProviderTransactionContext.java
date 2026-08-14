package com.xjtu.iron.transaction.provider.spring.context;

import com.xjtu.iron.transaction.api.context.TransactionParticipation;
import com.xjtu.iron.transaction.spi.provider.ProviderTransactionContext;
import org.springframework.transaction.TransactionStatus;

import java.util.Objects;

/**
 * 把 Spring TransactionStatus 收敛成组件自己的最小 SPI 上下文。
 */
public final class SpringProviderTransactionContext implements ProviderTransactionContext {

    private final TransactionStatus status;
    private final TransactionParticipation participation;

    public SpringProviderTransactionContext(TransactionStatus status, TransactionParticipation participation) {
        this.status = Objects.requireNonNull(status, "status");
        this.participation = Objects.requireNonNull(participation, "participation");
    }

    @Override
    public TransactionParticipation participation() { return participation; }

    @Override
    public boolean isRollbackOnly() {
        // 直接读取 Spring TransactionStatus，保持 rollback-only 状态的实时性。
        return status.isRollbackOnly();
    }

    @Override
    public void setRollbackOnly() {
        // 由 Spring 决定 OWNER 是本地回滚、PARTICIPANT 是如何影响外层事务。
        status.setRollbackOnly();
    }
}
