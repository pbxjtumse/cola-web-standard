package com.xjtu.iron.concurrency.core.execution;

import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 线程池注册中心。
 *
 * <p>负责保存所有由并行组件创建和管理的 ThreadPoolExecutor。</p>
 */
public interface ThreadPoolRegistry {

    /**
     * 按名称获取线程池。
     *
     * @param executorName 线程池名称
     * @return 线程池
     */
    ThreadPoolExecutor getExecutor(String executorName);

    /**
     * 注册线程池。
     *
     * @param executorName 线程池名称
     * @param executor 线程池实例
     */
    void register(String executorName, ThreadPoolExecutor executor);

    /**
     * 返回线程池注册表快照。
     *
     * <p>注意：这里返回的是当前注册表的只读 Map 视图，value 仍然是真实 ThreadPoolExecutor。</p>
     * <p>监控 Gauge 需要引用真实 executor 才能实时读取数据；对外诊断接口应优先使用 ThreadPoolManager#snapshots。</p>
     *
     * @return 线程池注册表快照
     */
    Map<String, ThreadPoolExecutor> snapshot();
}
