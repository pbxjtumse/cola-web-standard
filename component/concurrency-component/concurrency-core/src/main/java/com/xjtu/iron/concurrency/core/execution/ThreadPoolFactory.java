package com.xjtu.iron.concurrency.core.execution;

import com.xjtu.iron.concurrency.api.execution.ThreadPoolSpec;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 线程池工厂。
 *
 * <p>根据 ThreadPoolSpec 创建 JDK ThreadPoolExecutor。</p>
 */
public interface ThreadPoolFactory {

    /**
     * 创建线程池。
     *
     * @param spec 线程池规格
     * @return JDK ThreadPoolExecutor
     */
    ThreadPoolExecutor create(ThreadPoolSpec spec);
}
