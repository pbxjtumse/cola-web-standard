package com.xjtu.iron.concurrency.config;


import com.xjtu.iron.concurrency.api.execution.pool.ThreadPoolSpec;

import java.util.Map;

/**
 * 线程池配置解析器。
 */
public interface ThreadPoolSpecResolver {

    ThreadPoolSpec resolve(String poolName);

    Map<String, ThreadPoolSpec> resolveAll();
}
