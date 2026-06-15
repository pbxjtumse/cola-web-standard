package com.xjtu.iron.concurrency.core.metrics;

import com.xjtu.iron.concurrency.api.event.TaskExecutionEvent;

/**
 * 并发组件指标记录器。
 */
public interface ConcurrencyMetricsRecorder {

    /** 记录任务提交。 */
    void recordSubmitted(TaskExecutionEvent event);

    /** 记录任务开始执行。 */
    void recordStarted(TaskExecutionEvent event);

    /** 记录任务执行成功。 */
    void recordSuccess(TaskExecutionEvent event);

    /** 记录任务执行失败。 */
    void recordFailure(TaskExecutionEvent event);

    /** 记录任务被拒绝。 */
    void recordRejected(TaskExecutionEvent event);

    /** 记录任务超时。 */
    void recordTimeout(TaskExecutionEvent event);

    /** 记录任务取消。 */
    void recordCancelled(TaskExecutionEvent event);

    /** 记录 fallback 触发初始化 */
    void recordFallback(TaskExecutionEvent event);

    /** 记录 fallback 触发成功或失败。 */
    void recordFallbackSuccess(TaskExecutionEvent event);

    /** 记录 fallback 触发失败。 */
    void recordFallbackFailure(TaskExecutionEvent event);

    /** 记录任务最终完成。 */
    void recordCompleted(TaskExecutionEvent event);
}
