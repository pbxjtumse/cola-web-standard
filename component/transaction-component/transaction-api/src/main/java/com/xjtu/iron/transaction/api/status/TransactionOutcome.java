package com.xjtu.iron.transaction.api.status;

/**
 * 事务基础设施能够明确表达的异常结果。
 *
 * <p>一期不再为正常 REQUIRED 嵌套调用制造 CREATED/JOINED、OWNER/PARTICIPANT 等公共概念。
 * 正常执行只返回业务值；只有基础设施异常时，才需要用 outcome 表达“已知回滚”、
 * “提交结果未知”等可靠性语义。</p>
 */
public enum TransactionOutcome {

    /**
     * 底层明确告诉调用方事务最终发生了回滚，例如 Spring 抛出 UnexpectedRollbackException。
     */
    ROLLED_BACK,

    /**
     * commit 阶段出现基础设施异常，调用方无法可靠判断数据库最终是否已经提交。
     */
    COMMIT_UNKNOWN,

    /**
     * 事务基础设施失败，但无法得到更精确的最终状态。
     */
    FAILED
}
