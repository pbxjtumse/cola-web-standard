package com.xjtu.iron.relational.spi;

import java.sql.SQLException;

/**
 * 为一次 Relational 执行获取可用 ConnectionHandle。
 *
 * <p>默认实现可从 DataSource 新建 OWNED Connection；事务感知实现可复用当前事务绑定的
 * Connection，并返回 BORROWED ConnectionHandle。</p>
 */
public interface ConnectionProvider {

    ConnectionHandle acquire(SqlExecutionContext context) throws SQLException;
}
