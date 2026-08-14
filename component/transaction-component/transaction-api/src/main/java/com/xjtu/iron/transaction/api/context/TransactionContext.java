package com.xjtu.iron.transaction.api.context;

/**
 * 一次 {@code TransactionExecutor.execute(...)} 调用中的事务上下文。
 *
 * <p>{@link #executionId()} 是组件生成的逻辑执行标识，不是 MySQL trx_id 或 PostgreSQL xid。</p>
 */
public interface TransactionContext {

    String executionId();

    String transactionName();

    TransactionParticipation participation();

    default boolean isNewTransaction() {
        // OWNER 表示当前 execute 真正创建了一个新的物理事务。
        return participation() == TransactionParticipation.OWNER;
    }

    default boolean isParticipating() {
        // PARTICIPANT 表示当前 execute 只是加入外层事务，返回时不能宣称数据库已经提交。
        return participation() == TransactionParticipation.PARTICIPANT;
    }

    boolean isRollbackOnly();

    /**
     * 标记当前事务最终必须回滚。
     *
     * <p>如果当前执行只是 REQUIRED 加入外层事务，这个标记会传播到外层物理事务。</p>
     */
    void setRollbackOnly();
}
