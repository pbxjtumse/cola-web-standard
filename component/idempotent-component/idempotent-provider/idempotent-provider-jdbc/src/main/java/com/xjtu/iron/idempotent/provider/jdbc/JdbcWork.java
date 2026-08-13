package com.xjtu.iron.idempotent.provider.jdbc;

import java.sql.Connection;

/** JDBC Connection 上执行的一段工作。 */
@FunctionalInterface
public interface JdbcWork<T> {
    T execute(Connection connection) throws Exception;
}
