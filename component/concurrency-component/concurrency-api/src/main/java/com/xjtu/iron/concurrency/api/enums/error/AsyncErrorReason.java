package com.xjtu.iron.concurrency.api.enums.error;


/**
 * 异步错误具体原因。
 *
 * <p>
 * category 表达错误大类，reason 表达具体原因。
 * </p>
 */
public enum AsyncErrorReason {

    /**
     * 无错误。
     */
    NONE,

    /**
     * 用户提交的任务逻辑抛出异常。
     */
    TASK_THROWN,

    /**
     * 线程池拒绝任务。
     */
    REJECTED,

    /**
     * 任务结果超时。
     */
    TIMEOUT,

    /**
     * 任务排队超时。
     */
    QUEUE_TIMEOUT,

    /**
     * 任务被取消。
     */
    CANCELLED,

    /**
     * fallback 执行过程中抛出异常。
     */
    FALLBACK_THROWN,

    /**
     * 指定线程池不存在。
     */
    EXECUTOR_NOT_FOUND,

    /**
     * 未知原因。
     */
    UNKNOWN
}
