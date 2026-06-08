package com.xjtu.iron.concurrency.core.metrics;

import java.util.Collections;
import java.util.Map;

/**
 * 并发组件指标记录器。
 *
 * <p>这个接口用于记录任务维度指标。线程池维度 Gauge 指标由 ThreadPoolMetricName + Micrometer Gauge 负责。</p>
 *
 * <p>这里的方法都提供默认实现，避免业务侧自定义实现因为组件升级而大量破坏。</p>
 */
public interface ConcurrencyMetricsRecorder {

    /**
     * 记录任务提交。
     *
     * @param executorName 线程池名称
     * @param taskName 任务名称
     */
    default void recordSubmitted(String executorName, String taskName) {
        recordSubmitted(executorName, taskName, Collections.emptyMap());
    }

    /**
     * 记录任务提交。
     *
     * @param executorName 线程池名称
     * @param taskName 任务名称
     * @param tags 任务标签
     */
    default void recordSubmitted(String executorName, String taskName, Map<String, String> tags) {
    }

    /**
     * 记录任务开始执行。
     *
     * @param executorName 线程池名称
     * @param taskName 任务名称
     * @param queueCostMillis 排队耗时
     * @param tags 任务标签
     */
    default void recordStarted(String executorName, String taskName, long queueCostMillis, Map<String, String> tags) {
    }

    /**
     * 记录任务成功，兼容旧方法。
     *
     * @param executorName 线程池名称
     * @param taskName 任务名称
     * @param costMillis 总耗时
     */
    default void recordSuccess(String executorName, String taskName, long costMillis) {
        recordSuccess(executorName, taskName, 0L, costMillis, costMillis, Collections.emptyMap());
    }

    /**
     * 记录任务成功。
     *
     * @param executorName 线程池名称
     * @param taskName 任务名称
     * @param queueCostMillis 排队耗时
     * @param runCostMillis 执行耗时
     * @param totalCostMillis 总耗时
     * @param tags 任务标签
     */
    default void recordSuccess(
            String executorName,
            String taskName,
            long queueCostMillis,
            long runCostMillis,
            long totalCostMillis,
            Map<String, String> tags
    ) {
    }

    /**
     * 记录任务失败，兼容旧方法。
     *
     * @param executorName 线程池名称
     * @param taskName 任务名称
     * @param costMillis 总耗时
     * @param ex 异常对象
     */
    default void recordFailure(String executorName, String taskName, long costMillis, Throwable ex) {
        recordFailure(executorName, taskName, 0L, costMillis, costMillis, ex, Collections.emptyMap());
    }

    /**
     * 记录任务失败。
     *
     * @param executorName 线程池名称
     * @param taskName 任务名称
     * @param queueCostMillis 排队耗时
     * @param runCostMillis 执行耗时
     * @param totalCostMillis 总耗时
     * @param ex 异常对象
     * @param tags 任务标签
     */
    default void recordFailure(
            String executorName,
            String taskName,
            long queueCostMillis,
            long runCostMillis,
            long totalCostMillis,
            Throwable ex,
            Map<String, String> tags
    ) {
    }

    /**
     * 记录任务拒绝。
     *
     * @param executorName 线程池名称
     * @param taskName 任务名称
     */
    default void recordRejected(String executorName, String taskName) {
        recordRejected(executorName, taskName, Collections.emptyMap());
    }

    /**
     * 记录任务拒绝。
     *
     * @param executorName 线程池名称
     * @param taskName 任务名称
     * @param tags 任务标签
     */
    default void recordRejected(String executorName, String taskName, Map<String, String> tags) {
    }

    /**
     * 记录任务超时。
     *
     * @param executorName 线程池名称
     * @param taskName 任务名称
     * @param elapsedMillis 已耗时
     * @param tags 任务标签
     */
    default void recordTimeout(String executorName, String taskName, long elapsedMillis, Map<String, String> tags) {
    }

    /**
     * 记录任务 fallback。
     *
     * @param executorName 线程池名称
     * @param taskName 任务名称
     * @param tags 任务标签
     */
    default void recordFallback(String executorName, String taskName, Map<String, String> tags) {
    }
}
