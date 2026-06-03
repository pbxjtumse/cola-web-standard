package com.xjtu.iron.concurrency.core.metrics;

/**
 * 并发组件指标记录器。
 */
public interface ConcurrencyMetricsRecorder {

    void recordSubmitted(String executorName, String taskName);

    void recordSuccess(String executorName, String taskName, long costMillis);

    void recordFailure(String executorName, String taskName, long costMillis, Throwable ex);

    void recordRejected(String executorName, String taskName);
}