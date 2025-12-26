package com.xjtu.iron.cola.web.factory;

import com.alibaba.ttl.threadpool.TtlExecutors;
import com.xjtu.iron.cola.web.config.ThreadPoolConfig;

import java.util.concurrent.*;


/**
 * 线程池工厂
 * @author pbxjt
 * @date 2025/12/26
 */
public final class ThreadPoolFactory {

    /**
     * @param config
     * @return {@link ExecutorService }
     */
    public static ExecutorService create(ThreadPoolConfig config) {
        BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(config.getQueueSize());
        ThreadFactory threadFactory = r -> new Thread(r, "tp-" + config.getName());
        ThreadPoolExecutor executor =
                new ThreadPoolExecutor(
                        config.getCoreSize(),
                        config.getMaxSize(),
                        config.getKeepAliveSeconds(),
                        TimeUnit.SECONDS,
                        queue,
                        threadFactory,
                        RejectedPolicyFactory.create(config.getRejectPolicy())
                );

        ExecutorService result = executor;
        if (config.isEnableTtl()) {
            result = TtlExecutors.getTtlExecutorService(executor);
        }
        return result;
    }
}
