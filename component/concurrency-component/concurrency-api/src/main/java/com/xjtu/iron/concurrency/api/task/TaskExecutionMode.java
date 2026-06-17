package com.xjtu.iron.concurrency.api.task;

/**
 * 原始任务实际执行方式。
 *
 * <p>
 * 该枚举描述用户提交的原始 {@code Runnable/Supplier} 最终由哪一类线程执行，
 * 不描述 fallback 的执行线程。fallback 属于结果恢复管道，由独立执行器负责。
 * </p>
 */
public enum TaskExecutionMode {

    /**
     * 尚未确定执行方式。
     *
     * <p>任务已创建或已提交，但还没有真正进入 {@code run()}。</p>
     */
    UNASSIGNED,

    /**
     * 由配置的 {@link java.util.concurrent.ThreadPoolExecutor} 工作线程执行。
     */
    THREAD_POOL,

    /**
     * 因 {@code CALLER_RUNS} 拒绝策略，由调用 {@code execute(...)} 的提交线程直接执行。
     *
     * <p>
     * 这是一种反压执行方式，不代表任务失败，也不应把任务状态标记为 REJECTED。
     * </p>
     */
    CALLER_THREAD
}
