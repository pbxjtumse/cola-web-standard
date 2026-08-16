package com.xjtu.iron.idempotent.core.transaction;

/**
 * transaction-component 基础设施失败后，幂等 Core 真正关心的最小结果语义。
 */
public enum IdempotencyTransactionOutcome {

    /** 底层明确表示事务最终回滚。 */
    ROLLED_BACK,

    /** commit 调用失败，调用方无法确认最终是否已经提交。 */
    COMMIT_UNKNOWN,

    /** 事务基础设施失败，但无法得到更精确的最终结果。 */
    FAILED
}
