package com.xjtu.iron.concurrency.api.enums.error;


/**
 * 异步错误发生阶段。
 *
 * <p>
 * 用于描述错误发生在任务生命周期的哪个阶段。
 * </p>
 */
public enum AsyncErrorStage {

    /**
     * 无错误阶段。
     */
    NONE,

    /**
     * 任务提交阶段。
     *
     * <p>
     * 例如获取线程池失败、线程池拒绝任务。
     * </p>
     */
    SUBMIT,

    /**
     * 任务排队阶段。
     *
     * <p>
     * 例如任务在线程池队列中等待过久。
     * </p>
     */
    QUEUE,

    /**
     * 任务执行阶段。
     *
     * <p>
     * 例如用户提交的 Runnable/Supplier 抛出异常。
     * </p>
     */
    RUN,

    /**
     * 等待结果阶段。
     *
     * <p>
     * 例如 CompletableFuture 结果层超时。
     * </p>
     */
    WAIT_RESULT,

    /**
     * fallback 执行阶段。
     */
    FALLBACK,

    /**
     * 任务取消阶段。
     */
    CANCEL
}
