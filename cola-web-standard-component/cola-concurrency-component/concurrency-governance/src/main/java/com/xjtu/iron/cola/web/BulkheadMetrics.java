package com.xjtu.iron.cola.web;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class BulkheadMetrics {

    private final MeterRegistry registry;

    public BulkheadMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void bind(String name, Bulkhead bulkhead) {
        Gauge.builder("bulkhead.inuse", bulkhead, Bulkhead::getInUse)
                .tag("name", name)
                .register(registry);

        Gauge.builder("bulkhead.limit", bulkhead, Bulkhead::getLimit)
                .tag("name", name)
                .register(registry);
    }
}
