package com.xjtu.iron.concurrency.api.enums;

/**
 * 异步任务状态。
 */
public enum AsyncTaskStatus {

    /**
     * 提交成功。
     */
    SUBMITTED,

    /**
     * 执行成功。
     */
    SUCCESS,

    /**
     * 执行失败。
     */
    FAILED,

    /**
     * 执行超时。
     */
    TIMEOUT,

    /**
     * 任务被线程池拒绝。
     */
    REJECTED,

    /**
     * 执行了 fallback。
     */
    FALLBACK
}
