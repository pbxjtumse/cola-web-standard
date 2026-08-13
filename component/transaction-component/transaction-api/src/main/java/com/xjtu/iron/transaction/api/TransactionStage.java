package com.xjtu.iron.transaction.api;

/**
 * 事务执行阶段，用于错误定位、事件与后续可观测性。
 */
public enum TransactionStage {
    VALIDATE,
    RESOLVE,
    BEGIN,
    EXECUTE,
    COMMIT,
    ROLLBACK,
    COMPLETION
}
