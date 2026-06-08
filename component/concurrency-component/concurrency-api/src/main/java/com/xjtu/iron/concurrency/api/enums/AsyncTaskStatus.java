package com.xjtu.iron.concurrency.api.enums;

/**
 * 异步任务状态。
 *
 * <p>这个枚举用于任务监听器、任务执行事件、任务诊断与指标统计。</p>
 */
public enum AsyncTaskStatus {

    /**
     * 任务已提交到并行组件，准备进入线程池。
     */
    SUBMITTED,

    /**
     * 任务已经被线程池工作线程取出并开始执行。
     */
    RUNNING,

    /**
     * 任务执行成功。
     */
    SUCCESS,

    /**
     * 任务执行失败。
     */
    FAILED,

    /**
     * 任务发生超时。
     *
     * <p>包含结果层超时和排队超时。</p>
     */
    TIMEOUT,

    /**
     * 任务被线程池拒绝。
     */
    REJECTED,

    /**
     * 任务执行了 fallback 降级逻辑。
     */
    FALLBACK,

    /**
     * 任务已经进入终态。
     *
     * <p>监听器的 onCompleted 可以使用这个状态表达任务生命周期结束。</p>
     */
    COMPLETED
}
