package com.xjtu.iron.concurrency.api.enums;

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
