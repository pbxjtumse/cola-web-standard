package com.xjtu.iron.transaction.api;

/**
 * 事务事件监听器。
 *
 * <p>监听器属于观测扩展点，不允许其异常反向破坏业务事务流程。
 * DefaultTransactionExecutor 会隔离监听器异常。</p>
 */
@FunctionalInterface
public interface TransactionEventListener {

    void onEvent(TransactionEvent event);
}
