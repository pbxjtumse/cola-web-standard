package com.xjtu.iron.concurrency.core.metrics;

public class NoopConcurrencyMetricsRecorder implements ConcurrencyMetricsRecorder {

    @Override
    public void recordSubmitted(String executorName, String taskName) {
    }

    @Override
    public void recordSuccess(String executorName, String taskName, long costMillis) {
    }

    @Override
    public void recordFailure(String executorName, String taskName, long costMillis, Throwable ex) {
    }

    @Override
    public void recordRejected(String executorName, String taskName) {
    }
}
