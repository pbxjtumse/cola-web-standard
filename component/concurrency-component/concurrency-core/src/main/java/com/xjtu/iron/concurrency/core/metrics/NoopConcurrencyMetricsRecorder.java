package com.xjtu.iron.concurrency.core.metrics;

/**
 * 空指标记录器。
 *
 * <p>用于业务没有接入 Micrometer 或其他指标系统时保证组件仍然可以独立运行。</p>
 */
public class NoopConcurrencyMetricsRecorder implements ConcurrencyMetricsRecorder {
}
