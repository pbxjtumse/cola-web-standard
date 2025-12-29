package com.xjtu.iron.cola.web.metric;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

import java.util.concurrent.ThreadPoolExecutor;

public class ThreadPoolMetricsBinder {

    public static void bind(MeterRegistry registry, String poolName, ThreadPoolExecutor executor) {
        Gauge.builder("executor.active", executor, ThreadPoolExecutor::getActiveCount)
                .tag("pool", poolName)
                .register(registry);

        Gauge.builder("executor.pool.size", executor, ThreadPoolExecutor::getPoolSize)
                .tag("pool", poolName)
                .register(registry);

        Gauge.builder("executor.queue.size", executor,
                        e -> e.getQueue().size())
                .tag("pool", poolName)
                .register(registry);
    }
}
