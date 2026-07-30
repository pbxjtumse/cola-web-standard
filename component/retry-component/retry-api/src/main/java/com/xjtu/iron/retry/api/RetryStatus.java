package com.xjtu.iron.retry.api;

/**
 * 一次逻辑重试执行的最终状态。
 */
public enum RetryStatus {

    /**
     * 在允许的尝试范围内成功。
     */
    SUCCESS,

    /**
     * 已达到最大尝试次数，仍未取得成功结果。
     */
    EXHAUSTED,

    /**
     * 当前异常或返回结果被分类为不可重试。
     */
    NOT_RETRYABLE,

    /**
     * 总持续时间预算不足，不能继续下一次尝试或等待。
     */
    TIMED_OUT,

    /**
     * 执行线程被中断。
     */
    INTERRUPTED,

    /**
     * 为后续异步重试预留的取消状态。
     */
    CANCELLED,

    /**
     * 为后续重试预算能力预留的拒绝状态。
     */
    BUDGET_REJECTED,

    /**
     * 执行器、分类器或退避策略出现非业务异常。
     */
    EXECUTION_FAILED
}
