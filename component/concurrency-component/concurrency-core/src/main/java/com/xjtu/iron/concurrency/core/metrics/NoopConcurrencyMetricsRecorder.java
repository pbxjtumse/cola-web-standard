package com.xjtu.iron.concurrency.core.metrics;

import com.xjtu.iron.concurrency.api.event.TaskExecutionEvent;

/**
 * 空指标记录器。
 */
public class NoopConcurrencyMetricsRecorder implements ConcurrencyMetricsRecorder {

    @Override
    public void recordSubmitted(TaskExecutionEvent event) {
    }

    @Override
    public void recordStarted(TaskExecutionEvent event) {
    }

    @Override
    public void recordSuccess(TaskExecutionEvent event) {
    }

    @Override
    public void recordFailure(TaskExecutionEvent event) {
    }

    @Override
    public void recordRejected(TaskExecutionEvent event) {
    }

    @Override
    public void recordTimeout(TaskExecutionEvent event) {
    }

    @Override
    public void recordCancelled(TaskExecutionEvent event) {
    }

    @Override
    public void recordFallback(TaskExecutionEvent event) {
    }

    @Override
    public void recordFallbackSuccess(TaskExecutionEvent event) {

    }

    @Override
    public void recordFallbackFailure(TaskExecutionEvent event) {

    }

    @Override
    public void recordCompleted(TaskExecutionEvent event) {
    }
}
