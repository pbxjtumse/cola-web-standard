package com.xjtu.iron.transaction.api;

/**
 * 一次 TransactionExecutor.execute 调用中的事务上下文。
 *
 * <p>{@link #executionId()} 是组件生成的逻辑执行标识，不是 MySQL trx_id、
 * PostgreSQL xid 等数据库物理事务 ID。</p>
 */
public interface TransactionContext {

    String executionId();

    String transactionName();

    TransactionParticipation participation();

    default boolean isNewTransaction() {
        return participation() == TransactionParticipation.OWNER;
    }

    default boolean isParticipating() {
        return participation() == TransactionParticipation.PARTICIPANT;
    }

    boolean isRollbackOnly();

    /**
     * 将当前事务标记为 rollback-only。
     *
     * <p>若当前执行只是 REQUIRED 加入外层事务，则该标记会影响外层物理事务的最终结果。</p>
     */
    void setRollbackOnly();
}
