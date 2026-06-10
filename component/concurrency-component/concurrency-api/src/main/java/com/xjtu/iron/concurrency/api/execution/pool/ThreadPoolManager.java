package com.xjtu.iron.concurrency.api.execution.pool;

import java.util.Map;

/**
 * 线程池运行时管理器。
 *
 * <p>用于查询线程池快照，以及运行时调整线程池 core/max 等参数。</p>
 */
public interface ThreadPoolManager {

    /** 查询单个线程池诊断快照。 */
    ThreadPoolSnapshot snapshot(String executorName);

    /** 查询所有线程池诊断快照。 */
    Map<String, ThreadPoolSnapshot> snapshots();

    /** 调整线程池 core/max。 */
    ThreadPoolSnapshot resize(String executorName, int corePoolSize, int maximumPoolSize);

    /** 按请求对象更新线程池运行时参数。 */
    ThreadPoolSnapshot update(String executorName, ThreadPoolUpdateRequest request);
}
