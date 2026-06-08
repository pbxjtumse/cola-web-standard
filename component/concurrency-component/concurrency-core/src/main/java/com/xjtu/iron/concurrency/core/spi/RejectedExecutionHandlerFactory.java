package com.xjtu.iron.concurrency.core.spi;

import com.xjtu.iron.concurrency.api.execution.ThreadPoolSpec;

import java.util.concurrent.RejectedExecutionHandler;

/**
 * 拒绝策略工厂扩展点。
 */
public interface RejectedExecutionHandlerFactory {

    /** 根据线程池配置创建拒绝策略。 */
    RejectedExecutionHandler create(ThreadPoolSpec spec);
}
