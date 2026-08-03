package com.xjtu.iron.retry.api.event;

/** 定义重试生命周期中可以被监听的事件类型。 */
public enum RetryEventType {
    /** 一次逻辑重试执行已经创建。 */
    EXECUTION_STARTED,
    /** 发现非幂等操作配置了多次尝试。 */
    SAFETY_WARNING,
    /** 一次物理尝试即将开始。 */
    ATTEMPT_STARTED,
    /** 一次物理尝试已经结束。 */
    ATTEMPT_COMPLETED,
    /** 分类器已经给出下一步决策。 */
    DECISION_MADE,
    /** 下一次重试已经计算好等待时间。 */
    RETRY_SCHEDULED,
    /** 逻辑执行最终成功。 */
    EXECUTION_SUCCEEDED,
    /** 最大尝试次数已经耗尽。 */
    EXECUTION_EXHAUSTED,
    /** 当前失败或结果不可重试。 */
    EXECUTION_NOT_RETRYABLE,
    /** 总时长预算已经耗尽。 */
    EXECUTION_TIMED_OUT,
    /** 当前线程或等待过程被中断。 */
    EXECUTION_INTERRUPTED,
    /** 调用方请求取消。 */
    EXECUTION_CANCELLED,
    /** 分类器要求立即终止。 */
    EXECUTION_ABORTED,
    /** 重试基础设施发生内部错误。 */
    EXECUTION_FAILED
}
