package com.xjtu.iron.transaction.provider.spring.context;

import com.xjtu.iron.transaction.spi.provider.ProviderTransactionContext;
import org.springframework.transaction.TransactionStatus;

import java.util.Objects;

/**
 * 把 Spring TransactionStatus 收敛成组件自己的最小 SPI 上下文。
 */
public final class SpringProviderTransactionContext implements ProviderTransactionContext {

    private final TransactionStatus status;

    public SpringProviderTransactionContext(TransactionStatus status) {
        this.status = Objects.requireNonNull(status, "status");
    }

    @Override
    public boolean isRollbackOnly() {
        // 始终读取 Spring TransactionStatus 的实时 rollback-only 标志。
        return status.isRollbackOnly();
    }

    @Override
    public void setRollbackOnly() {
        // 业务只表达“最终必须回滚”；具体底层事务传播与完成语义仍由 Spring TransactionManager 决定。
        status.setRollbackOnly();
    }
}
