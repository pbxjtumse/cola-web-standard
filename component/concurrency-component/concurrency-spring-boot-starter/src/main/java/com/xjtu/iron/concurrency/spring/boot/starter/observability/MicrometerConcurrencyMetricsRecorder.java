package com.xjtu.iron.concurrency.spring.boot.starter.observability;

import com.xjtu.iron.concurrency.api.event.TaskExecutionEvent;
import com.xjtu.iron.concurrency.core.metrics.ConcurrencyMetricsRecorder;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;

import java.util.concurrent.TimeUnit;

/**
 * 基于 Micrometer 的任务指标记录器。
 */
public class MicrometerConcurrencyMetricsRecorder implements ConcurrencyMetricsRecorder {

    private final MeterRegistry meterRegistry;

    public MicrometerConcurrencyMetricsRecorder(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void recordSubmitted(TaskExecutionEvent event) {
        counter("xjtu.iron.concurrency.task.submitted", event).increment();
    }

    @Override
    public void recordStarted(TaskExecutionEvent event) {
        counter("xjtu.iron.concurrency.task.started", event).increment();
    }

    @Override
    public void recordSuccess(TaskExecutionEvent event) {
        counter("xjtu.iron.concurrency.task.success", event).increment();
        recordTimers(event);
    }

    @Override
    public void recordFailure(TaskExecutionEvent event) {
        counter("xjtu.iron.concurrency.task.failure", event).increment();
        recordTimers(event);
    }

    @Override
    public void recordRejected(TaskExecutionEvent event) {
        counter("xjtu.iron.concurrency.task.rejected", event).increment();
    }

    @Override
    public void recordTimeout(TaskExecutionEvent event) {
        counter("xjtu.iron.concurrency.task.timeout", event).increment();
    }

    @Override
    public void recordCancelled(TaskExecutionEvent event) {
        counter("xjtu.iron.concurrency.task.cancelled", event).increment();
    }

    @Override
    public void recordFallback(TaskExecutionEvent event) {
        counter("xjtu.iron.concurrency.task.fallback", event).increment();
    }

    @Override
    public void recordCompleted(TaskExecutionEvent event) {
        counter("xjtu.iron.concurrency.task.completed", event).increment();
    }

    private Counter counter(String name, TaskExecutionEvent event) {
        return Counter.builder(name)
                .tags(tags(event))
                .register(meterRegistry);
    }

    private void recordTimers(TaskExecutionEvent event) {
        timer("xjtu.iron.concurrency.task.queue.cost", event).record(Math.max(event.getQueueCostMillis(), 0), TimeUnit.MILLISECONDS);
        timer("xjtu.iron.concurrency.task.run.cost", event).record(Math.max(event.getRunCostMillis(), 0), TimeUnit.MILLISECONDS);
        timer("xjtu.iron.concurrency.task.total.cost", event).record(Math.max(event.getTotalCostMillis(), 0), TimeUnit.MILLISECONDS);
    }

    private Timer timer(String name, TaskExecutionEvent event) {
        return Timer.builder(name)
                .tags(tags(event))
                .register(meterRegistry);
    }

    private Tags tags(TaskExecutionEvent event) {
        return Tags.of(
                "component", "xjtu-iron-concurrency",
                "executor", safe(event.getExecutorName()),
                "task", safe(event.getTaskName()),
                "status", event.getStatus() == null ? "unknown" : event.getStatus().name()
        );
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }
}
