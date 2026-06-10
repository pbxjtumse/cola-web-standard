package com.xjtu.iron.concurrency.api.enums.task;

/**
 * 异步任务状态。
 * | 状态          | 谁设置                                           | 什么时候设置                   |
 * | ----------- | --------------------------------------------- | ------------------------ |
 * | `CREATED`   | `AsyncTask` / registry                        | 任务对象刚创建，可选               |
 * | `SUBMITTED` | `DefaultTaskExecutionTemplate`                | 任务准备提交给线程池               |
 * | `RUNNING`   | `TaskCommand.run()`                           | 工作线程真正开始执行               |
 * | `SUCCESS`   | `TaskCommand.run()`                           | supplier/runnable 正常执行完成 |
 * | `FAILED`    | `TaskCommand.run()`                           | supplier/runnable 抛异常    |
 * | `REJECTED`  | `TaskCommand.reject()`                        | 线程池拒绝任务                  |
 * | `TIMEOUT`   | `DefaultTaskExecutionTemplate`                | `withTimeout` 触发结果层超时    |
 * | `CANCELLED` | `TaskCommand.cancelRunning()` / Future cancel | 用户取消或超时后尝试取消             |
 * | `FALLBACK`  | `DefaultTaskExecutionTemplate`                | fallback 被执行并返回降级结果      |
 */
/**
 * 异步任务状态。
 *
 * <p>
 * 用于描述异步任务当前所处的生命周期阶段，或者最终以什么状态结束。
 * </p>
 */
public enum AsyncTaskStatus {

    /**
     * 任务已创建，但还没有提交到线程池。
     */
    CREATED,

    /**
     * 任务已经提交给线程池。
     */
    SUBMITTED,

    /**
     * 任务已经被线程池工作线程取出，并开始执行。
     */
    RUNNING,

    /**
     * 任务正常执行成功。
     */
    SUCCESS,

    /**
     * 任务执行失败。
     *
     * <p>
     * 一般表示用户提交的 Runnable/Supplier 在执行过程中抛出了异常。
     * 具体错误详情通过 AsyncError 表达。
     * </p>
     */
    FAILED,

    /**
     * 任务被线程池拒绝。
     */
    REJECTED,

    /**
     * 任务从调用方视角已经超时。
     */
    TIMEOUT,

    /**
     * 任务被取消。
     */
    CANCELLED,

    /**
     * 原始任务失败或超时后，fallback 执行成功。
     */
    FALLBACK_SUCCESS,

    /**
     * 原始任务失败或超时后，fallback 执行也失败。
     */
    FALLBACK_FAILED
}
