package com.xjtu.iron.transaction.api.status;

/**
 * 一次事务逻辑执行已知的最终结果。
 */
public enum TransactionOutcome {
    /** 当前调用拥有的新事务已确认提交。 */
    COMMITTED,
    /** 当前调用拥有的新事务已确认回滚。 */
    ROLLED_BACK,
    /** 当前调用只参与外层事务，不能声称已经发生物理提交。 */
    PARTICIPATED,
    /** 当前参与事务已被标记 rollback-only，最终由外层完成回滚。 */
    ROLLBACK_ONLY,
    /** commit 阶段异常，本地调用方无法可靠确认数据库最终状态。 */
    COMMIT_UNKNOWN,
    /** 事务基础设施失败，且没有更精确的结果。 */
    FAILED
}
