package com.xjtu.iron.concurrency.core.execution;

import com.xjtu.iron.concurrency.api.exception.ThreadPoolNotFoundException;
import com.xjtu.iron.concurrency.core.spi.ThreadPoolRegistry;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 默认线程池注册中心。
 */
public class DefaultThreadPoolRegistry implements ThreadPoolRegistry {

    private final Map<String, ThreadPoolExecutor> executors = new ConcurrentHashMap<>();

    @Override
    public ThreadPoolExecutor getExecutor(String executorName) {
        ThreadPoolExecutor executor = executors.get(executorName);
        if (executor == null) {
            throw new ThreadPoolNotFoundException(executorName);
        }
        return executor;
    }

    @Override
    public void register(String executorName, ThreadPoolExecutor executor) {
        executors.put(executorName, executor);
    }

    @Override
    public Map<String, ThreadPoolExecutor> snapshot() {
        return Collections.unmodifiableMap(executors);
    }
}
