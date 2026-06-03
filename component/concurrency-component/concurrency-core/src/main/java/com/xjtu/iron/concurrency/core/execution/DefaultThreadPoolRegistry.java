package com.xjtu.iron.concurrency.core.execution;

import com.xjtu.iron.concurrency.api.exception.ThreadPoolNotFoundException;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

/**
 * 默认线程池注册中心。
 */
public class DefaultThreadPoolRegistry implements ThreadPoolRegistry {

    /**
     * 线程池集合。
     *
     * <p>key 是线程池名称。</p>
     */
    private final Map<String, ExecutorService> executors = new ConcurrentHashMap<>();

    @Override
    public ExecutorService getExecutor(String executorName) {
        ExecutorService executor = executors.get(executorName);

        if (executor == null) {
            throw new ThreadPoolNotFoundException(executorName);
        }

        return executor;
    }

    @Override
    public void register(String executorName, ExecutorService executorService) {
        executors.put(executorName, executorService);
    }

    @Override
    public Map<String, ExecutorService> snapshot() {
        return Collections.unmodifiableMap(executors);
    }
}