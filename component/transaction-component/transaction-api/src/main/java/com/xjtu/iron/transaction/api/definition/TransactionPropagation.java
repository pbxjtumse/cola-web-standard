package com.xjtu.iron.transaction.api.definition;

/**
 * 一期支持的事务传播行为。
 */
public enum TransactionPropagation {
    /** 有事务则加入，没有事务则创建。 */
    REQUIRED,
    /** 始终创建新事务；已有事务由底层事务管理器挂起，完成后恢复。 */
    REQUIRES_NEW,
    /** 必须存在事务，否则立即失败。 */
    MANDATORY
}
