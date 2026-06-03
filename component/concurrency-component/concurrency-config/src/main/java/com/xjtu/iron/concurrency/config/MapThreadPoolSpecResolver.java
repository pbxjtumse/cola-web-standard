package com.xjtu.iron.concurrency.config;

import com.xjtu.iron.concurrency.api.execution.ThreadPoolSpec;

import java.util.Map;

public class MapThreadPoolSpecResolver implements ThreadPoolSpecResolver {

    private final Map<String, ThreadPoolSpec> specs;

    public MapThreadPoolSpecResolver(Map<String, ThreadPoolSpec> specs) {
        this.specs = specs == null ? Map.of() : Map.copyOf(specs);
    }

    @Override
    public ThreadPoolSpec resolve(String poolName) {
        return specs.get(poolName);
    }

    @Override
    public Map<String, ThreadPoolSpec> resolveAll() {
        return specs;
    }
}
