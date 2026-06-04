package com.xjtu.iron.concurrency.api.execution;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 线程池运行时快照。
 *
 * <p>这是一个“某一瞬间”的不可变诊断对象，适合暴露给监控、健康检查、管理接口。</p>
 */
public class ThreadPoolSnapshot {

    /** 线程池名称。 */
    private final String executorName;

    /** 核心线程数。 */
    private final int corePoolSize;

    /** 最大线程数。 */
    private final int maximumPoolSize;

    /** 当前实际线程数。 */
    private final int poolSize;

    /** 线程池历史最大线程数。 */
    private final int largestPoolSize;

    /** 当前正在执行任务的线程数。 */
    private final int activeCount;

    /** 当前队列中等待执行的任务数量。 */
    private final int queueSize;

    /** 队列剩余容量。 */
    private final int queueRemainingCapacity;

    /** 队列总容量估算值。 */
    private final int queueCapacity;

    /** 活跃线程使用率，activeCount / maximumPoolSize。 */
    private final double activeUsageRatio;

    /** 队列使用率，queueSize / queueCapacity。 */
    private final double queueUsageRatio;

    /** 已完成任务总数。 */
    private final long completedTaskCount;

    /** 线程池曾经接收过的任务总数。 */
    private final long taskCount;

    /** 线程池是否已经 shutdown。 */
    private final boolean shutdown;

    /** 线程池是否正在终止。 */
    private final boolean terminating;

    /** 线程池是否已经完全终止。 */
    private final boolean terminated;

    /** 当前拒绝策略实现类名称。 */
    private final String rejectedExecutionHandler;

    /**
     * 创建线程池运行时快照。
     */
    public ThreadPoolSnapshot(
            String executorName,
            int corePoolSize,
            int maximumPoolSize,
            int poolSize,
            int largestPoolSize,
            int activeCount,
            int queueSize,
            int queueRemainingCapacity,
            int queueCapacity,
            double activeUsageRatio,
            double queueUsageRatio,
            long completedTaskCount,
            long taskCount,
            boolean shutdown,
            boolean terminating,
            boolean terminated,
            String rejectedExecutionHandler
    ) {
        this.executorName = executorName;
        this.corePoolSize = corePoolSize;
        this.maximumPoolSize = maximumPoolSize;
        this.poolSize = poolSize;
        this.largestPoolSize = largestPoolSize;
        this.activeCount = activeCount;
        this.queueSize = queueSize;
        this.queueRemainingCapacity = queueRemainingCapacity;
        this.queueCapacity = queueCapacity;
        this.activeUsageRatio = activeUsageRatio;
        this.queueUsageRatio = queueUsageRatio;
        this.completedTaskCount = completedTaskCount;
        this.taskCount = taskCount;
        this.shutdown = shutdown;
        this.terminating = terminating;
        this.terminated = terminated;
        this.rejectedExecutionHandler = rejectedExecutionHandler;
    }

    /**
     * 从 ThreadPoolExecutor 当前状态生成快照。
     *
     * @param executorName 线程池名称
     * @param executor JDK 线程池
     * @return 线程池快照
     */
    public static ThreadPoolSnapshot from(String executorName, ThreadPoolExecutor executor) {
        int queueSize = executor.getQueue().size();
        int remainingCapacity = executor.getQueue().remainingCapacity();
        int queueCapacity = safeQueueCapacity(queueSize, remainingCapacity);

        double activeUsageRatio = executor.getMaximumPoolSize() <= 0
                ? 0D
                : (double) executor.getActiveCount() / executor.getMaximumPoolSize();

        double queueUsageRatio = queueCapacity <= 0 || queueCapacity == Integer.MAX_VALUE
                ? 0D
                : (double) queueSize / queueCapacity;

        return new ThreadPoolSnapshot(
                executorName,
                executor.getCorePoolSize(),
                executor.getMaximumPoolSize(),
                executor.getPoolSize(),
                executor.getLargestPoolSize(),
                executor.getActiveCount(),
                queueSize,
                remainingCapacity,
                queueCapacity,
                activeUsageRatio,
                queueUsageRatio,
                executor.getCompletedTaskCount(),
                executor.getTaskCount(),
                executor.isShutdown(),
                executor.isTerminating(),
                executor.isTerminated(),
                executor.getRejectedExecutionHandler().getClass().getName()
        );
    }

    /**
     * 安全估算队列总容量。
     *
     * @param queueSize 当前队列大小
     * @param remainingCapacity 队列剩余容量
     * @return 队列容量估算值
     */
    private static int safeQueueCapacity(int queueSize, int remainingCapacity) {
        if (remainingCapacity == Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        long capacity = (long) queueSize + remainingCapacity;
        return capacity > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) capacity;
    }

    /** @return 线程池名称 */
    public String getExecutorName() { return executorName; }

    /** @return 核心线程数 */
    public int getCorePoolSize() { return corePoolSize; }

    /** @return 最大线程数 */
    public int getMaximumPoolSize() { return maximumPoolSize; }

    /** @return 当前实际线程数 */
    public int getPoolSize() { return poolSize; }

    /** @return 历史最大线程数 */
    public int getLargestPoolSize() { return largestPoolSize; }

    /** @return 当前活跃线程数 */
    public int getActiveCount() { return activeCount; }

    /** @return 当前队列大小 */
    public int getQueueSize() { return queueSize; }

    /** @return 队列剩余容量 */
    public int getQueueRemainingCapacity() { return queueRemainingCapacity; }

    /** @return 队列容量估算值 */
    public int getQueueCapacity() { return queueCapacity; }

    /** @return 活跃线程使用率 */
    public double getActiveUsageRatio() { return activeUsageRatio; }

    /** @return 队列使用率 */
    public double getQueueUsageRatio() { return queueUsageRatio; }

    /** @return 已完成任务数 */
    public long getCompletedTaskCount() { return completedTaskCount; }

    /** @return 任务总数 */
    public long getTaskCount() { return taskCount; }

    /** @return 是否已 shutdown */
    public boolean isShutdown() { return shutdown; }

    /** @return 是否正在终止 */
    public boolean isTerminating() { return terminating; }

    /** @return 是否已终止 */
    public boolean isTerminated() { return terminated; }

    /** @return 拒绝策略实现类名称 */
    public String getRejectedExecutionHandler() { return rejectedExecutionHandler; }
}
