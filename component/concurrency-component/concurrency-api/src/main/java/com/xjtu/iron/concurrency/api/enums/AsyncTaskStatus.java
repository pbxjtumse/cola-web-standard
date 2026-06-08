package com.xjtu.iron.concurrency.api.enums;

/**
 * 异步任务状态。
 */
public enum AsyncTaskStatus {

    /** 任务已创建但尚未提交。 */
    PENDING,

    /** 任务已提交到线程池。 */
    SUBMITTED,

    /** 任务正在执行。 */
    RUNNING,

    /** 任务执行成功。 */
    SUCCESS,

    /** 任务执行失败。 */
    FAILED,

    /** 任务执行超时。 */
    TIMEOUT,

    /** 任务被取消。 */
    CANCELLED,

    /** 任务被线程池拒绝。 */
    REJECTED,

    /** 任务执行了 fallback。 */
    FALLBACK
}
