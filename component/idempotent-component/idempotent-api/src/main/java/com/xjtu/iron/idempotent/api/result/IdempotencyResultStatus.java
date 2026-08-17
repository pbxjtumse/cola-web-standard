package com.xjtu.iron.idempotent.api.result;

import com.xjtu.iron.idempotent.api.execution.IdempotencyExecutor;

/**
 * 一次 {@link IdempotencyExecutor} 调用返回给上层的最终结果。
 *
 * <p>这些值不是数据库持久状态，而是“这一次调用最终发生了什么”。</p>
 */
public enum IdempotencyResultStatus {

    /** 普通 execute() 抢占成功并完成业务。 */
    EXECUTED,

    /** recover() 抢占成功并完成恢复业务。 */
    RECOVERED,

    /** 记录已经 SUCCESS，本次没有重执业务，而是返回历史结果/历史成功语义。 */
    REPLAYED,

    /** 已有 PROCESSING 且执行权仍未过期。 */
    PROCESSING,

    /** 已有 PROCESSING，但 processingExpireAt 已经过期；普通 execute() 不自动接管。 */
    PROCESSING_EXPIRED,

    /** 历史 FAILED，且 failureRetryable=true；普通 execute() 仍不自动恢复。 */
    PREVIOUS_FAILED_RETRYABLE,

    /** 历史 FAILED，且不允许恢复。 */
    PREVIOUS_FAILED_FINAL,

    /** 当前 Options 或记录不允许 recover()。 */
    RECOVERY_NOT_ALLOWED,

    /** 扫描任务携带的 expectedOwner/version 已经过时。 */
    STALE_RECOVERY_CANDIDATE,

    /** 同一个幂等 Key 被不同 requestHash / routeKey 复用。 */
    KEY_CONFLICT,

    /** 可选分布式锁没有获取成功，且配置为不允许 fallback。 */
    LOCK_NOT_ACQUIRED,

    /** callback 执行失败。 */
    EXECUTION_FAILED,

    /** Tx-B 本地事务明确没有成功完成，例如 BEGIN 失败或最终明确回滚。 */
    TRANSACTION_FAILED,

    /** Tx-B 在 COMMIT 阶段出现结果不确定；调用方不能假设业务一定提交或一定回滚。 */
    TRANSACTION_COMMIT_UNKNOWN,

    /** callback 执行完后发现 owner/version 已失效，不能提交最终状态。 */
    OWNERSHIP_LOST,

    /** ResultPolicy 在保存 SNAPSHOT/REFERENCE 或执行 replay 时失败。 */
    RESULT_POLICY_ERROR,

    /** 当前调用要求 SNAPSHOT/REFERENCE replay，但历史 SUCCESS 没有可用 payload。 */
    RESULT_REPLAY_UNAVAILABLE,

    /** 历史 payload 使用的 ResultPolicy 类型与当前调用不一致。 */
    RESULT_POLICY_MISMATCH,

    /**
     * @deprecated V1.3 请使用 RESULT_POLICY_ERROR。
     */
    @Deprecated
    RESULT_CODEC_ERROR,

    /** Repository/Provider 访问失败。 */
    REPOSITORY_ERROR,

    /** 请求或策略参数非法。 */
    INVALID_OPTIONS
}
