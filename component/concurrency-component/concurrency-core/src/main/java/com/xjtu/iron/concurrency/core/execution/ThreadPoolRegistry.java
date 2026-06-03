package com.xjtu.iron.concurrency.core.execution;

import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * 线程池注册中心。
 */
public interface ThreadPoolRegistry {

    ExecutorService getExecutor(String executorName);

    void register(String executorName, ExecutorService executorService);

    Map<String, ExecutorService> snapshot();
}