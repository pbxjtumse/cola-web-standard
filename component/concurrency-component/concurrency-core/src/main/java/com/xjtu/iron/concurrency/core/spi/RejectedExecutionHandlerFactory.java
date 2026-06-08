package com.xjtu.iron.concurrency.core.spi;

import com.xjtu.iron.concurrency.api.execution.ThreadPoolSpec;

import java.util.concurrent.RejectedExecutionHandler;

/**
 * 拒绝策略工厂 SPI。
 *
 * <p>用于根据线程池配置创建 JDK RejectedExecutionHandler。</p>
 */
public interface RejectedExecutionHandlerFactory {

    /**
     * 创建拒绝策略处理器。
     *
     * @param spec 线程池配置
     * @return 拒绝策略处理器
     */
    RejectedExecutionHandler create(ThreadPoolSpec spec);
}
