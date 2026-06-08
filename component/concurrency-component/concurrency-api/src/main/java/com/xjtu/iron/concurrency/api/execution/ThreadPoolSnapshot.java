package com.xjtu.iron.concurrency.api.execution;

/**
 * 线程池某一时刻的诊断快照。
 */
public class ThreadPoolSnapshot {

    /** 线程池名称。 */
    private String executorName;

    /** 核心线程数。 */
    private int corePoolSize;

    /** 最大线程数。 */
    private int maximumPoolSize;

    /** 当前线程池线程数。 */
    private int poolSize;

    /** 历史最大线程数。 */
    private int largestPoolSize;

    /** 当前活跃线程数。 */
    private int activeCount;

    /** 队列当前任务数。 */
    private int queueSize;

    /** 队列剩余容量。 */
    private int queueRemainingCapacity;

    /** 队列总容量。 */
    private int queueCapacity;

    /** 活跃线程使用率：activeCount / maximumPoolSize。 */
    private double activeUsageRatio;

    /** 队列使用率：queueSize / queueCapacity。 */
    private double queueUsageRatio;

    /** 已完成任务数。 */
    private long completedTaskCount;

    /** 总任务数。 */
    private long taskCount;

    /** 是否已关闭。 */
    private boolean shutdown;

    /** 是否正在终止。 */
    private boolean terminating;

    /** 是否已终止。 */
    private boolean terminated;

    /** 拒绝策略名称。 */
    private String rejectedExecutionHandler;

    /** 是否忙碌，由诊断阈值判断。 */
    private boolean busy;

    public String getExecutorName() {
        return executorName;
    }

    public void setExecutorName(String executorName) {
        this.executorName = executorName;
    }

    public int getCorePoolSize() {
        return corePoolSize;
    }

    public void setCorePoolSize(int corePoolSize) {
        this.corePoolSize = corePoolSize;
    }

    public int getMaximumPoolSize() {
        return maximumPoolSize;
    }

    public void setMaximumPoolSize(int maximumPoolSize) {
        this.maximumPoolSize = maximumPoolSize;
    }

    public int getPoolSize() {
        return poolSize;
    }

    public void setPoolSize(int poolSize) {
        this.poolSize = poolSize;
    }

    public int getLargestPoolSize() {
        return largestPoolSize;
    }

    public void setLargestPoolSize(int largestPoolSize) {
        this.largestPoolSize = largestPoolSize;
    }

    public int getActiveCount() {
        return activeCount;
    }

    public void setActiveCount(int activeCount) {
        this.activeCount = activeCount;
    }

    public int getQueueSize() {
        return queueSize;
    }

    public void setQueueSize(int queueSize) {
        this.queueSize = queueSize;
    }

    public int getQueueRemainingCapacity() {
        return queueRemainingCapacity;
    }

    public void setQueueRemainingCapacity(int queueRemainingCapacity) {
        this.queueRemainingCapacity = queueRemainingCapacity;
    }

    public int getQueueCapacity() {
        return queueCapacity;
    }

    public void setQueueCapacity(int queueCapacity) {
        this.queueCapacity = queueCapacity;
    }

    public double getActiveUsageRatio() {
        return activeUsageRatio;
    }

    public void setActiveUsageRatio(double activeUsageRatio) {
        this.activeUsageRatio = activeUsageRatio;
    }

    public double getQueueUsageRatio() {
        return queueUsageRatio;
    }

    public void setQueueUsageRatio(double queueUsageRatio) {
        this.queueUsageRatio = queueUsageRatio;
    }

    public long getCompletedTaskCount() {
        return completedTaskCount;
    }

    public void setCompletedTaskCount(long completedTaskCount) {
        this.completedTaskCount = completedTaskCount;
    }

    public long getTaskCount() {
        return taskCount;
    }

    public void setTaskCount(long taskCount) {
        this.taskCount = taskCount;
    }

    public boolean isShutdown() {
        return shutdown;
    }

    public void setShutdown(boolean shutdown) {
        this.shutdown = shutdown;
    }

    public boolean isTerminating() {
        return terminating;
    }

    public void setTerminating(boolean terminating) {
        this.terminating = terminating;
    }

    public boolean isTerminated() {
        return terminated;
    }

    public void setTerminated(boolean terminated) {
        this.terminated = terminated;
    }

    public String getRejectedExecutionHandler() {
        return rejectedExecutionHandler;
    }

    public void setRejectedExecutionHandler(String rejectedExecutionHandler) {
        this.rejectedExecutionHandler = rejectedExecutionHandler;
    }

    public boolean isBusy() {
        return busy;
    }

    public void setBusy(boolean busy) {
        this.busy = busy;
    }
}
