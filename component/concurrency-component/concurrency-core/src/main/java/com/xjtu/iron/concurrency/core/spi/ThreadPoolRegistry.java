package com.xjtu.iron.concurrency.core.spi;

import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 线程池注册中心 SPI。
 *
 * <p>负责保存组件创建的所有 ThreadPoolExecutor，并给投递、监控、诊断模块查询使用。</p>
 */
public interface ThreadPoolRegistry {

    /**
     * 根据名称获取线程池。
     *
     * @param executorName 线程池名称
     * @return ThreadPoolExecutor
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
     * 获取注册表快照。
     *
     * <p>返回的是不可变 Map，但 value 仍然是运行中的 ThreadPoolExecutor 实例。这个方法主要用于监控 Gauge 绑定和运行时诊断。</p>
     *
     * @return 线程池注册表快照
     */
    Map<String, ThreadPoolExecutor> snapshot();

    /**
     * 动态替换线程池
     * @param name
     * @param expectedOld
     * @param newExecutor
     */
    void replace(String name, ThreadPoolExecutor expectedOld, ThreadPoolExecutor newExecutor);
}
