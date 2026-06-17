package com.xjtu.iron.concurrency.core.task;

/**
 * 调用方线程执行感知接口。
 *
 * <p>
 * CALLER_RUNS 拒绝处理器在直接调用 {@link Runnable#run()} 之前通过该接口通知任务，
 * 使生命周期事件和任务快照能够记录 CALLER_THREAD 执行模式。
 * </p>
 */
public interface CallerRunsAware {

    /**
     * 标记当前任务将由提交线程直接执行。
     */
    void markCallerThreadExecution();
}
