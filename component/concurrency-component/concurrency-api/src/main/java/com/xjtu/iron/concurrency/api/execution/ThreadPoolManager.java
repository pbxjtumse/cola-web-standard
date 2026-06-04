package com.xjtu.iron.concurrency.api.execution;

import java.util.Map;

/**
 * 线程池运行时管理接口。
 *
 * <p>这个接口用于支持诊断、监控、手工扩容、配置中心联动。</p>
 *
 * <p>注意：这里管理的是 JDK {@code ThreadPoolExecutor} 的运行时可变属性，
 * 不是完整重建线程池。队列类型、队列容量、线程工厂这类创建期属性不在一期运行时更新范围内。</p>
 */
public interface ThreadPoolManager {

    /**
     * 获取单个线程池的运行时快照。
     *
     * @param executorName 线程池名称
     * @return 线程池运行时快照
     */
    ThreadPoolSnapshot snapshot(String executorName);

    /**
     * 获取所有线程池的运行时快照。
     *
     * @return key 为线程池名称，value 为运行时快照
     */
    Map<String, ThreadPoolSnapshot> snapshots();

    /**
     * 调整线程池核心线程数和最大线程数。
     *
     * @param executorName 线程池名称
     * @param corePoolSize 新核心线程数
     * @param maximumPoolSize 新最大线程数
     * @return 调整后的运行时快照
     */
    ThreadPoolSnapshot resize(String executorName, int corePoolSize, int maximumPoolSize);

    /**
     * 按请求对象更新线程池运行时参数。
     *
     * @param executorName 线程池名称
     * @param request 更新请求
     * @return 更新后的运行时快照
     */
    ThreadPoolSnapshot update(String executorName, ThreadPoolUpdateRequest request);
}
