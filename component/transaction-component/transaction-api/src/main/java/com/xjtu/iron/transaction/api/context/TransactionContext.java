package com.xjtu.iron.transaction.api.context;

/**
 * 一次 {@code TransactionExecutor.execute(...)} 调用中的稳定事务上下文。
 *
 * <p>{@link #executionId()} 是事务组件生成的逻辑执行标识，
 * 只用于日志、事件和链路关联，不是 MySQL trx_id、PostgreSQL xid 或其他数据库事务编号。</p>
 *
 * <p>一期刻意不暴露“是否新建物理事务”等 Spring 内部传播细节。
 * 业务只需要知道自己处在事务边界中，并可以主动将当前事务标记为 rollback-only。</p>
 */
public interface TransactionContext {

    String executionId();

    String transactionName();

    boolean isRollbackOnly();

    /**
     * 将当前 Spring 事务状态标记为 rollback-only。
     *
     * <p>如果 REQUIRED 正在复用外部事务，该标记会影响同一个底层事务；
     * 具体如何完成回滚继续交给底层 TransactionManager 处理。</p>
     */
    void setRollbackOnly();
}
