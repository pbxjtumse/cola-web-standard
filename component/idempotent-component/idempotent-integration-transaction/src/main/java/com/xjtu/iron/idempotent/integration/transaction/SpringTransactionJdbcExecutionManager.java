package com.xjtu.iron.idempotent.integration.transaction;

import com.xjtu.iron.idempotent.provider.jdbc.JdbcExecutionManager;
import com.xjtu.iron.idempotent.provider.jdbc.JdbcWork;
import com.xjtu.iron.transaction.api.definition.TransactionOptions;
import com.xjtu.iron.transaction.api.definition.TransactionPropagation;
import com.xjtu.iron.transaction.api.execution.TransactionExecutor;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Objects;

/**
 * transaction-component + Spring 事务资源绑定下的 JDBC 执行管理器。
 *
 * <p>这是幂等 JDBC Provider 真正进入事务模板的关键桥梁：</p>
 * <pre>
 * Tx-A / Tx-C:
 *   TransactionExecutor(REQUIRES_NEW)
 *       -> Spring TransactionManager 绑定 Connection
 *       -> DataSourceUtils 取得该 Connection
 *
 * Tx-B:
 *   IdempotencyTransactionCoordinator(REQUIRED)
 *       -> business SQL
 *       -> markSuccess()
 *       -> inCurrentTransaction()
 *       -> 复用同一个 transaction-bound Connection
 * </pre>
 *
 * <p>这里不能使用 {@code dataSource.getConnection()}，否则 markSuccess 很可能拿到 Connection-B，
 * 与业务 Connection-A 分离，最终失去原子性。</p>
 */
public final class SpringTransactionJdbcExecutionManager implements JdbcExecutionManager {

    private static final String STATE_TRANSACTION_NAME = "idempotency-state";

    private final DataSource dataSource;
    private final TransactionExecutor transactionExecutor;

    public SpringTransactionJdbcExecutionManager(DataSource dataSource, TransactionExecutor transactionExecutor) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
        this.transactionExecutor = Objects.requireNonNull(transactionExecutor, "transactionExecutor must not be null");
    }

    @Override
    public <T> T withConnection(JdbcWork<T> work) throws Exception {
        Objects.requireNonNull(work, "work must not be null");
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try {
            return work.execute(connection);
        } finally {
            // 如果 Connection 属于当前事务，releaseConnection 不会提前 close；
            // 如果只是普通查询 Connection，则会正常归还连接池。
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    @Override
    public <T> T inCurrentTransaction(JdbcWork<T> work) throws Exception {
        Objects.requireNonNull(work, "work must not be null");

        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException(
                    "markSuccess requires an active local transaction, but no transaction is active");
        }

        Connection connection = DataSourceUtils.getConnection(dataSource);
        try {
            if (!DataSourceUtils.isConnectionTransactional(connection, dataSource)) {
                throw new IllegalStateException(
                        "current transaction does not bind the idempotency DataSource; "
                                + "business DB and idempotency DB must use the same local transaction resource");
            }
            return work.execute(connection);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    @Override
    public <T> T inNewTransaction(JdbcWork<T> work) throws Exception {
        Objects.requireNonNull(work, "work must not be null");

        TransactionOptions options = TransactionOptions.builder()
                .name(STATE_TRANSACTION_NAME)
                .propagation(TransactionPropagation.REQUIRES_NEW)
                .build();

        try {
            return transactionExecutor.execute(options, context -> {
                try {
                    // REQUIRES_NEW 已经由 transaction-component 建立真实新事务；
                    // 这里必须取得那个事务绑定的 Connection，而不是自己 connection.setAutoCommit(false)。
                    return inCurrentTransaction(work);
                } catch (RuntimeException | Error unchecked) {
                    throw unchecked;
                } catch (Exception checked) {
                    throw new CheckedJdbcWorkRuntimeException(checked);
                }
            });
        } catch (CheckedJdbcWorkRuntimeException checked) {
            throw checked.original;
        }
    }

    @Override
    public boolean supportsCurrentTransactionParticipation() {
        return true;
    }

    private static final class CheckedJdbcWorkRuntimeException extends RuntimeException {
        private final Exception original;

        private CheckedJdbcWorkRuntimeException(Exception original) {
            super(original);
            this.original = original;
        }
    }
}
