package com.xjtu.iron.retry.config;

import com.xjtu.iron.retry.api.RetryEvent;
import com.xjtu.iron.retry.api.RetryEventType;
import com.xjtu.iron.retry.api.RetryListener;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 基于 Micrometer 的基础重试指标监听器。
 */
public final class MicrometerRetryListener implements RetryListener {

    private final MeterRegistry meterRegistry;
    private final AtomicInteger activeExecutions = new AtomicInteger();

    public MicrometerRetryListener(MeterRegistry meterRegistry) {
        this.meterRegistry = Objects.requireNonNull(meterRegistry, "meterRegistry must not be null");
        Gauge.builder("iron.retry.active", activeExecutions, AtomicInteger::get)
                .description("Current active logical retry executions")
                .register(meterRegistry);
    }

    @Override
    public void onEvent(RetryEvent event) {
        String operation = event.getOperationName();
        String policy = event.getPolicyName();

        if (event.getEventType() == RetryEventType.EXECUTION_STARTED) {
            activeExecutions.incrementAndGet();
            counter("iron.retry.execution.total", operation, policy, "started").increment();
            return;
        }

        if (event.getEventType() == RetryEventType.ATTEMPT_STARTED) {
            counter("iron.retry.attempt.total", operation, policy, "started").increment();
            return;
        }

        if (event.getEventType() == RetryEventType.ATTEMPT_COMPLETED) {
            Timer.builder("iron.retry.attempt.duration")
                    .description("Duration of one physical retry attempt")
                    .tags("operation", operation, "policy", policy)
                    .register(meterRegistry)
                    .record(event.getAttemptDuration().toNanos(), TimeUnit.NANOSECONDS);
            return;
        }

        if (event.getEventType() == RetryEventType.RETRY_SCHEDULED) {
            counter("iron.retry.scheduled.total", operation, policy, "scheduled").increment();
            Timer.builder("iron.retry.backoff.duration")
                    .description("Configured backoff duration before the next attempt")
                    .tags("operation", operation, "policy", policy)
                    .register(meterRegistry)
                    .record(event.getNextDelay().toNanos(), TimeUnit.NANOSECONDS);
            return;
        }

        if (isTerminal(event.getEventType())) {
            activeExecutions.updateAndGet(current -> Math.max(0, current - 1));
            String outcome = event.getFinalStatus() == null
                    ? event.getEventType().name().toLowerCase()
                    : event.getFinalStatus().name().toLowerCase();
            counter("iron.retry.outcome.total", operation, policy, outcome).increment();
            Timer.builder("iron.retry.execution.duration")
                    .description("Duration of one logical retry execution")
                    .tags("operation", operation, "policy", policy, "outcome", outcome)
                    .register(meterRegistry)
                    .record(event.getElapsedTime().toNanos(), TimeUnit.NANOSECONDS);
        }
    }

    private Counter counter(String name, String operation, String policy, String outcome) {
        return Counter.builder(name)
                .tags("operation", operation, "policy", policy, "outcome", outcome)
                .register(meterRegistry);
    }

    private boolean isTerminal(RetryEventType eventType) {
        return eventType == RetryEventType.SUCCEEDED
                || eventType == RetryEventType.EXHAUSTED
                || eventType == RetryEventType.NOT_RETRYABLE
                || eventType == RetryEventType.TIMED_OUT
                || eventType == RetryEventType.INTERRUPTED
                || eventType == RetryEventType.ABORTED;
    }
}
