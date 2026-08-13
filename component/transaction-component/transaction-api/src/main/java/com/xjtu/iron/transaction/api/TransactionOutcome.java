package com.xjtu.iron.transaction.api;

/**
 * 一次事务逻辑执行已知的最终结果。
 */
public enum TransactionOutcome {

    /** 当前调用拥有的新事务已确认提交。 */
    COMMITTED,

    /** 当前调用拥有的新事务已确认回滚。 */
    ROLLED_BACK,

    /** 当前调用仅参与外层事务，不能声称已经发生物理提交。 */
    PARTICIPATED,

    /** 当前参与事务已被标记 rollback-only，最终由外层完成实际回滚。 */
    ROLLBACK_ONLY,

    /** commit 阶段异常，无法从本地可靠确认数据库最终是否已经提交。 */
    COMMIT_UNKNOWN,

    /** 事务基础设施执行失败，未形成更精确的结果。 */
    FAILED
}
