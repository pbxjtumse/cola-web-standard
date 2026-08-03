package com.xjtu.iron.retry.api.execution;

/** 描述一次逻辑重试执行的最终状态。 */
public enum RetryStatus {
    /** 业务操作最终成功。 */
    SUCCESS,
    /** 分类器仍允许重试，但最大尝试次数已经耗尽。 */
    EXHAUSTED,
    /** 当前失败或结果被明确判定为不可重试。 */
    NOT_RETRYABLE,
    /** 重试总时长预算已经耗尽。 */
    TIMED_OUT,
    /** 当前线程或退避等待被中断。 */
    INTERRUPTED,
    /** 调用方通过协作式取消令牌请求取消。 */
    CANCELLED,
    /** 分类器明确要求立即终止。 */
    ABORTED,
    /** 分类器、退避策略、ID 生成器等内部基础设施执行失败。 */
    EXECUTION_FAILED
}
