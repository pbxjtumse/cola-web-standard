package com.xjtu.iron.concurrency.core.execution;

import com.xjtu.iron.concurrency.core.spi.ThreadPoolRegistry;
import com.xjtu.iron.concurrency.core.spi.RejectedExecutionHandlerFactory;
import com.xjtu.iron.concurrency.api.execution.ThreadPoolManager;
import com.xjtu.iron.concurrency.api.execution.ThreadPoolSnapshot;
import com.xjtu.iron.concurrency.api.execution.ThreadPoolSpec;
import com.xjtu.iron.concurrency.api.execution.ThreadPoolUpdateRequest;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 默认线程池运行时管理器。
 *
 * <p>支持 ThreadPoolExecutor 原生可变更项：core/max/keepAlive/allowCoreThreadTimeout/rejectionHandler。</p>
 */
public class DefaultThreadPoolManager implements ThreadPoolManager {

    /**
     * 线程池注册中心。
     */
    private final ThreadPoolRegistry threadPoolRegistry;

    /**
     * 拒绝策略工厂。
     */
    private final RejectedExecutionHandlerFactory rejectedExecutionHandlerFactory;

    /**
     * 创建默认线程池运行时管理器。
     *
     * @param threadPoolRegistry 线程池注册中心
     * @param rejectedExecutionHandlerFactory 拒绝策略工厂
     */
    public DefaultThreadPoolManager(
            ThreadPoolRegistry threadPoolRegistry,
            RejectedExecutionHandlerFactory rejectedExecutionHandlerFactory
    ) {
        this.threadPoolRegistry = threadPoolRegistry;
        this.rejectedExecutionHandlerFactory = rejectedExecutionHandlerFactory;
    }

    /**
     * 获取单个线程池运行时快照。
     *
     * @param executorName 线程池名称
     * @return 运行时快照
     */
    @Override
    public ThreadPoolSnapshot snapshot(String executorName) {
        ThreadPoolExecutor executor = threadPoolRegistry.getExecutor(executorName);
        return ThreadPoolSnapshot.from(executorName, executor);
    }

    /**
     * 获取所有线程池运行时快照。
     *
     * @return 所有线程池快照
     */
    @Override
    public Map<String, ThreadPoolSnapshot> snapshots() {
        Map<String, ThreadPoolSnapshot> result = new LinkedHashMap<>();
        threadPoolRegistry.snapshot().forEach((name, executor) -> result.put(name, ThreadPoolSnapshot.from(name, executor)));
        return result;
    }

    /**
     * 调整线程池核心线程数和最大线程数。
     *
     * @param executorName 线程池名称
     * @param corePoolSize 核心线程数
     * @param maximumPoolSize 最大线程数
     * @return 调整后的线程池快照
     */
    @Override
    public ThreadPoolSnapshot resize(String executorName, int corePoolSize, int maximumPoolSize) {
        return update(executorName, ThreadPoolUpdateRequest.resize(corePoolSize, maximumPoolSize));
    }

    /**
     * 按请求对象更新线程池运行时配置。
     *
     * @param executorName 线程池名称
     * @param request 更新请求
     * @return 更新后的线程池快照
     */
    @Override
    public ThreadPoolSnapshot update(String executorName, ThreadPoolUpdateRequest request) {
        if (request == null) {
            return snapshot(executorName);
        }

        ThreadPoolExecutor executor = threadPoolRegistry.getExecutor(executorName);

        int targetCorePoolSize = request.getCorePoolSize() == null
                ? executor.getCorePoolSize()
                : request.getCorePoolSize();

        int targetMaximumPoolSize = request.getMaximumPoolSize() == null
                ? executor.getMaximumPoolSize()
                : request.getMaximumPoolSize();

        validatePoolSize(targetCorePoolSize, targetMaximumPoolSize);
        updatePoolSize(executor, targetCorePoolSize, targetMaximumPoolSize);

        if (request.getKeepAliveTime() != null) {
            if (request.getKeepAliveTime().isNegative()) {
                throw new IllegalArgumentException("keepAliveTime must not be negative");
            }
            executor.setKeepAliveTime(request.getKeepAliveTime().toMillis(), TimeUnit.MILLISECONDS);
        }

        if (request.getAllowCoreThreadTimeout() != null) {
            executor.allowCoreThreadTimeOut(request.getAllowCoreThreadTimeout());
        }

        if (request.getRejectionPolicy() != null) {
            ThreadPoolSpec spec = new ThreadPoolSpec();
            spec.setName(executorName);
            spec.setCorePoolSize(targetCorePoolSize);
            spec.setMaxPoolSize(targetMaximumPoolSize);
            spec.setRejectionPolicy(request.getRejectionPolicy());
            if (request.getRejectionWaitTime() != null) {
                spec.setRejectionWaitTime(request.getRejectionWaitTime());
            }
            spec.validate();
            executor.setRejectedExecutionHandler(rejectedExecutionHandlerFactory.create(spec));
        }

        if (Boolean.TRUE.equals(request.getPrestartAllCoreThreads())) {
            executor.prestartAllCoreThreads();
        }

        return ThreadPoolSnapshot.from(executorName, executor);
    }

    /**
     * 校验线程池大小是否合法。
     *
     * @param corePoolSize 核心线程数
     * @param maximumPoolSize 最大线程数
     */
    private void validatePoolSize(int corePoolSize, int maximumPoolSize) {
        if (corePoolSize <= 0) {
            throw new IllegalArgumentException("corePoolSize must be positive");
        }

        if (maximumPoolSize < corePoolSize) {
            throw new IllegalArgumentException("maximumPoolSize must be >= corePoolSize");
        }
    }

    /**
     * 更新线程池大小。
     *
     * <p>ThreadPoolExecutor 对 setCorePoolSize / setMaximumPoolSize 有顺序要求：</p>
     * <ul>
     *     <li>如果要调大 maximum，先调 maximum 再调 core。</li>
     *     <li>如果要调小 maximum，先调 core 再调 maximum。</li>
     * </ul>
     *
     * @param executor 线程池
     * @param targetCorePoolSize 目标核心线程数
     * @param targetMaximumPoolSize 目标最大线程数
     */
    private void updatePoolSize(ThreadPoolExecutor executor, int targetCorePoolSize, int targetMaximumPoolSize) {
        int currentMaximumPoolSize = executor.getMaximumPoolSize();

        if (targetMaximumPoolSize >= currentMaximumPoolSize) {
            executor.setMaximumPoolSize(targetMaximumPoolSize);
            executor.setCorePoolSize(targetCorePoolSize);
            return;
        }

        executor.setCorePoolSize(targetCorePoolSize);
        executor.setMaximumPoolSize(targetMaximumPoolSize);
    }
}
