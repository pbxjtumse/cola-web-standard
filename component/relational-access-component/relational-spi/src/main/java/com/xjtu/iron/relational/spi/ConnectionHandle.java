package com.xjtu.iron.relational.spi;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Connection 的资源句柄与所有权边界。
 *
 * <p>它解决的不是“再包装一次 JDBC”，而是明确谁有权关闭物理 Connection。
 * 事务集成返回 BORROWED；普通 DataSource 获取返回 OWNED。</p>
 */
public interface ConnectionHandle extends AutoCloseable {

    Connection connection();

    ConnectionOwnership ownership();

    /**
     * 释放本次访问对 Connection 的使用权。
     * OWNED 应物理关闭；BORROWED 仅逻辑释放。
     */
    @Override
    void close() throws SQLException;
}
