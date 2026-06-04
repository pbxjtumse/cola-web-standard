package com.xjtu.iron.concurrency.core.metrics;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.ToDoubleFunction;

/**
 * 线程池 Gauge 指标名称枚举。
 *
 * <p>这个枚举的目标不是为了炫技，而是为了把监控指标名称集中治理。</p>
 *
 * <p>如果指标名称散落在自动装配类里，后续改名、统一前缀、补充说明都很麻烦。
 * 统一放到枚举里以后，Observability 配置类只负责注册 Gauge，不负责维护指标字面量。</p>
 */
public enum ThreadPoolMetricName {

    /**
     * 当前正在执行任务的线程数。
     */
    ACTIVE_COUNT(
            "xjtu.iron.concurrency.thread.pool.active",
            "Active thread count",
            ThreadPoolExecutor::getActiveCount
    ),

    /**
     * 当前线程池里的实际线程数。
     */
    POOL_SIZE(
            "xjtu.iron.concurrency.thread.pool.size",
            "Current thread pool size",
            ThreadPoolExecutor::getPoolSize
    ),

    /**
     * 核心线程数。
     */
    CORE_POOL_SIZE(
            "xjtu.iron.concurrency.thread.pool.core.size",
            "Core pool size",
            ThreadPoolExecutor::getCorePoolSize
    ),

    /**
     * 最大线程数。
     */
    MAXIMUM_POOL_SIZE(
            "xjtu.iron.concurrency.thread.pool.max.size",
            "Maximum pool size",
            ThreadPoolExecutor::getMaximumPoolSize
    ),

    /**
     * 当前队列中等待执行的任务数量。
     */
    QUEUE_SIZE(
            "xjtu.iron.concurrency.thread.pool.queue.size",
            "Current queue size",
            executor -> executor.getQueue().size()
    ),

    /**
     * 队列剩余容量。
     */
    QUEUE_REMAINING_CAPACITY(
            "xjtu.iron.concurrency.thread.pool.queue.remaining",
            "Queue remaining capacity",
            executor -> executor.getQueue().remainingCapacity()
    ),

    /**
     * 已完成任务总数。
     */
    COMPLETED_TASK_COUNT(
            "xjtu.iron.concurrency.thread.pool.completed",
            "Completed task count",
            ThreadPoolExecutor::getCompletedTaskCount
    ),

    /**
     * 线程池曾经接收过的任务总数。
     */
    TASK_COUNT(
            "xjtu.iron.concurrency.thread.pool.task.count",
            "Total task count",
            ThreadPoolExecutor::getTaskCount
    );

    /**
     * Micrometer 指标名称。
     */
    private final String meterName;

    /**
     * 指标说明。
     */
    private final String description;

    /**
     * 从 ThreadPoolExecutor 提取指标值的函数。
     */
    private final ToDoubleFunction<ThreadPoolExecutor> valueFunction;

    /**
     * 创建线程池指标枚举。
     *
     * @param meterName 指标名称
     * @param description 指标说明
     * @param valueFunction 指标值提取函数
     */
    ThreadPoolMetricName(
            String meterName,
            String description,
            ToDoubleFunction<ThreadPoolExecutor> valueFunction
    ) {
        this.meterName = meterName;
        this.description = description;
        this.valueFunction = valueFunction;
    }

    /**
     * 返回 Micrometer 指标名称。
     *
     * @return 指标名称
     */
    public String meterName() {
        return meterName;
    }

    /**
     * 返回指标说明。
     *
     * @return 指标说明
     */
    public String description() {
        return description;
    }

    /**
     * 从线程池中读取当前指标值。
     *
     * @param executor JDK 线程池
     * @return 当前指标值
     */
    public double value(ThreadPoolExecutor executor) {
        return valueFunction.applyAsDouble(executor);
    }
}
