package com.xjtu.iron.concurrency.core.execution;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 线程池注册中心。
 */
public interface ThreadPoolRegistry {

    ThreadPoolExecutor getExecutor(String executorName);

    void register(String executorName, ExecutorService executorService);

    Map<String, ThreadPoolExecutor> snapshot();
}