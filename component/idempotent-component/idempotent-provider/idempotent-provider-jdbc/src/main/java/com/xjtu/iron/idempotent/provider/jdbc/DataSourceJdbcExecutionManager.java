package com.xjtu.iron.idempotent.provider.jdbc;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Objects;

/**
 * 不依赖 Spring Transaction 的默认 JDBC 执行管理器。
 *
 * <p>这只是 V1.1 默认实现：每次从 DataSource 直接获取 Connection。
 * 因此它<strong>不会</strong>自动参与未来外层 transaction-component 的事务。
 * 事务组件接入时应替换为 transaction-aware JdbcExecutionManager。</p>
 */
public final class DataSourceJdbcExecutionManager implements JdbcExecutionManager {

    private final DataSource dataSource;

    public DataSourceJdbcExecutionManager(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
    }

    @Override
    public <T> T withConnection(JdbcWork<T> work) throws Exception {
        // 这里直接从连接池取 Connection。
        // 即使外层未来存在 Spring Transaction，这个 Connection 也不一定是 transaction-bound Connection。
        // 因此当前实现不能保证 markSuccess 与业务 SQL 处于同一个物理事务。
        try (Connection connection = dataSource.getConnection()) {
            return work.execute(connection);
        }
    }

    @Override
    public <T> T inNewTransaction(JdbcWork<T> work) throws Exception {
        // tryAcquire / tryRecover 需要独立短事务：
        // PROCESSING 必须在真正业务 callback 开始前 COMMIT，让其他节点能够看见。
        try (Connection connection = dataSource.getConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                T result = work.execute(connection);
                connection.commit();
                return result;
            } catch (Throwable error) {
                connection.rollback();
                throw error;
            } finally {
                connection.setAutoCommit(originalAutoCommit);
            }
        }
    }
}
