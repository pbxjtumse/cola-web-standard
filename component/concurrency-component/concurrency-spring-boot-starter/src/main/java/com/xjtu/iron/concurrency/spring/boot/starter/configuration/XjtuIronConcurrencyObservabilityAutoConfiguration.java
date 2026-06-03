package com.xjtu.iron.concurrency.spring.boot.starter.configuration;

import com.xjtu.iron.concurrency.core.execution.ThreadPoolRegistry;
import com.xjtu.iron.concurrency.core.metrics.ConcurrencyMetricsRecorder;
import com.xjtu.iron.concurrency.core.metrics.NoopConcurrencyMetricsRecorder;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterBinder;
import io.micrometer.core.instrument.Tags;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

@AutoConfiguration(after = XjtuIronConcurrencyExecutionAutoConfiguration.class)
public class XjtuIronConcurrencyObservabilityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ConcurrencyMetricsRecorder concurrencyMetricsRecorder() {
        return new NoopConcurrencyMetricsRecorder();
    }

    @Bean(name = "ironConcurrencyThreadPoolMeterBinder")
    @ConditionalOnClass(name = "io.micrometer.core.instrument.MeterRegistry")
    @ConditionalOnMissingBean(name = "ironConcurrencyThreadPoolMeterBinder")
    public MeterBinder threadPoolMeterBinder(ThreadPoolRegistry threadPoolRegistry) {
        return meterRegistry -> threadPoolRegistry.snapshot().forEach((poolName, executorService) -> {
            if (!(executorService instanceof ThreadPoolExecutor executor)) {
                return;
            }

            Tags tags = Tags.of(
                    "component", "xjtu-iron-concurrency",
                    "pool", poolName
            );

            Gauge.builder("xjtu.iron.concurrency.thread.pool.active", executor, ThreadPoolExecutor::getActiveCount)
                    .description("Active thread count")
                    .tags(tags)
                    .register(meterRegistry);

            Gauge.builder("xjtu.iron.concurrency.thread.pool.size", executor, ThreadPoolExecutor::getPoolSize)
                    .description("Current thread pool size")
                    .tags(tags)
                    .register(meterRegistry);

            Gauge.builder("xjtu.iron.concurrency.thread.pool.core.size", executor, ThreadPoolExecutor::getCorePoolSize)
                    .description("Core pool size")
                    .tags(tags)
                    .register(meterRegistry);

            Gauge.builder("xjtu.iron.concurrency.thread.pool.max.size", executor, ThreadPoolExecutor::getMaximumPoolSize)
                    .description("Maximum pool size")
                    .tags(tags)
                    .register(meterRegistry);

            Gauge.builder("xjtu.iron.concurrency.thread.pool.queue.size", executor, e -> e.getQueue().size())
                    .description("Current queue size")
                    .tags(tags)
                    .register(meterRegistry);

            Gauge.builder("xjtu.iron.concurrency.thread.pool.queue.remaining", executor, e -> e.getQueue().remainingCapacity())
                    .description("Queue remaining capacity")
                    .tags(tags)
                    .register(meterRegistry);

            Gauge.builder("xjtu.iron.concurrency.thread.pool.completed", executor, ThreadPoolExecutor::getCompletedTaskCount)
                    .description("Completed task count")
                    .tags(tags)
                    .register(meterRegistry);

            Gauge.builder("xjtu.iron.concurrency.thread.pool.task.count", executor, ThreadPoolExecutor::getTaskCount)
                    .description("Total task count")
                    .tags(tags)
                    .register(meterRegistry);
        });
    }
}