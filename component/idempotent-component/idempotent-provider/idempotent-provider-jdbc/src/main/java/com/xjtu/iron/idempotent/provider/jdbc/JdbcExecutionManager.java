package com.xjtu.iron.idempotent.provider.jdbc;

/**
 * JDBC Connection 与事务参与方式的扩展点。
 *
 * <p>V2 把三类执行语义明确拆开：</p>
 * <ul>
 *     <li>{@link #withConnection(JdbcWork)}：普通查询/诊断，不要求当前必须存在事务；</li>
 *     <li>{@link #inCurrentTransaction(JdbcWork)}：Tx-B 中复用当前业务事务 Connection；</li>
 *     <li>{@link #inNewTransaction(JdbcWork)}：Tx-A / Tx-C 使用独立 REQUIRES_NEW 短事务。</li>
 * </ul>
 *
 * <p>这样 JDBC Repository 不需要知道 Spring、TransactionManager 或 transaction-component，
 * 只依赖这三个稳定语义。</p>
 */
public interface JdbcExecutionManager {

    /** 在普通 Connection 上执行；transaction-aware 实现可以安全复用已绑定 Connection。 */
    <T> T withConnection(JdbcWork<T> work) throws Exception;

    /**
     * 在当前业务事务 Connection 上执行。
     *
     * <p>默认实现退化为 {@link #withConnection(JdbcWork)}，用于保持未接入事务组件时的 V1.1 行为；
     * transaction-aware 实现应覆盖本方法，并在没有真实当前事务时 fail fast。</p>
     */
    default <T> T inCurrentTransaction(JdbcWork<T> work) throws Exception {
        return withConnection(work);
    }

    /**
     * 在独立短事务中执行。
     * tryAcquire / tryRecover / markFailed 应使用该方法。
     */
    <T> T inNewTransaction(JdbcWork<T> work) throws Exception;

    /**
     * 是否能够保证 inCurrentTransaction(...) 真正复用当前业务事务 Connection。
     *
     * <p>Core 只有在这里为 true 且 TransactionCoordinator 可用时，才会启用
     * “Business + markSuccess”原子事务闭环。</p>
     */
    default boolean supportsCurrentTransactionParticipation() {
        return false;
    }
}
