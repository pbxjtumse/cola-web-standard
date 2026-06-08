package com.xjtu.iron.concurrency.core.spi;

import com.xjtu.iron.concurrency.api.execution.ThreadPoolSpec;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 线程池工厂 SPI。
 *
 * <p>用于根据 {@link ThreadPoolSpec} 创建 JDK {@link ThreadPoolExecutor}。</p>
 */
public interface ThreadPoolFactory {

    /**
     * 创建线程池。
     *
     * @param spec 线程池配置
     * @return JDK ThreadPoolExecutor
     */
    ThreadPoolExecutor create(ThreadPoolSpec spec);
}
