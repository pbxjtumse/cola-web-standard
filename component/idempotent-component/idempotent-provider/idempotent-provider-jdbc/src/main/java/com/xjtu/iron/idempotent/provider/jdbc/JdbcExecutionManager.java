package com.xjtu.iron.idempotent.provider.jdbc;

/**
 * JDBC 连接/事务参与方式的扩展点。
 *
 * <p>当前默认实现直接基于 DataSource。未来 transaction-component 接入后，
 * 可提供 transaction-aware 实现，让 withConnection(...) 复用当前业务事务绑定的 Connection，
 * 让 markSuccess 与业务写入真正进入同一物理事务。</p>
 */
public interface JdbcExecutionManager {

    /** 在普通 Connection 上执行；transaction-aware 实现可复用当前事务 Connection。 */
    <T> T withConnection(JdbcWork<T> work) throws Exception;

    /**
     * 在独立短事务中执行。
     * tryAcquire / tryRecover 应使用该方法，避免把 PROCESSING 抢占持有到完整业务事务结束。
     */
    <T> T inNewTransaction(JdbcWork<T> work) throws Exception;
}
