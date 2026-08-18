package com.xjtu.iron.idempotent.provider.jdbc.execution;

/**
 * JDBC Connection 与事务参与方式的扩展点。
 *
 * <p>三类执行语义：</p>
 * <ul>
 *     <li>{@link #withConnection(JdbcWork)}：普通查询/诊断，不要求当前必须存在事务；</li>
 *     <li>{@link #inCurrentTransaction(JdbcWork)}：完成阶段使用当前执行上下文的 Connection；
 *         transaction-aware 实现必须复用 Tx-B 的 transaction-bound Connection；</li>
 *     <li>{@link #inNewTransaction(JdbcWork)}：Tx-A / Tx-C 使用独立短事务。</li>
 * </ul>
 *
 * <p>JDBC Repository 不感知 Spring、TransactionManager 或 transaction-component，
 * 只依赖这三个稳定语义。</p>
 */
public interface JdbcExecutionManager {

    /** 在普通 Connection 上执行；transaction-aware 实现可以安全复用已绑定 Connection。 */
    <T> T withConnection(JdbcWork<T> work) throws Exception;

    /**
     * 在完成阶段使用当前执行上下文的 Connection。
     *
     * <p>非 transaction-aware 实现可以显式使用普通 Connection；
     * transaction-aware 实现必须复用当前业务事务 Connection，并在事务不存在时 fail-fast。</p>
     */
    <T> T inCurrentTransaction(JdbcWork<T> work) throws Exception;

    /**
     * 在独立短事务中执行。
     * tryAcquire / tryRecover / markFailed 使用该方法。
     */
    <T> T inNewTransaction(JdbcWork<T> work) throws Exception;

    /**
     * 是否能够保证 inCurrentTransaction(...) 真正复用当前业务事务 Connection。
     *
     * <p>Core 只有在这里为 true 且 TransactionCoordinator 可用时，才启用
     * “Business + markSuccess”原子事务闭环。</p>
     */
    default boolean supportsCurrentTransactionParticipation() {
        return false;
    }
}
