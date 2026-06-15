package com.xjtu.iron.concurrency.core.task;

import java.util.concurrent.atomic.AtomicBoolean;


import com.xjtu.iron.concurrency.api.enums.task.AsyncTaskStatus;
import com.xjtu.iron.concurrency.api.task.TaskResultMode;
import com.xjtu.iron.concurrency.api.task.TaskTimingSnapshot;


import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 任务执行运行时状态。
 *
 * <p>
 * 该对象只在并行组件 core 内部使用，用于保存任务执行过程中动态变化的数据。
 * 它不能直接用于 REST 返回、Redis 存储或数据库持久化。
 * </p>
 */
public final class TaskExecutionRuntime {

    /**
     * 任务结果模式。
     */
    private final TaskResultMode resultMode;

    /**
     * 任务提交时的墙上时钟时间，毫秒。
     *
     * <p>
     * 用于对外展示时间点。
     * </p>
     */
    private final long submitTimeMillis;

    /**
     * 任务提交时的单调时钟值。
     *
     * <p>
     * 用于计算耗时，避免系统时钟回拨影响耗时结果。
     * </p>
     */
    private final long submitNanoTime;

    /**
     * 任务开始执行时的墙上时钟时间，毫秒。
     */
    private volatile long startTimeMillis;

    /**
     * 任务开始执行时的单调时钟值。
     */
    private volatile long startNanoTime;

    /**
     * 任务结束时的墙上时钟时间，毫秒。
     */
    private volatile long endTimeMillis;

    /**
     * 任务结束时的单调时钟值。
     */
    private volatile long endNanoTime;

    /**
     * 当前正在执行任务的线程。
     *
     * <p>
     * 用于超时或主动取消后尝试发送 interrupt。
     * </p>
     */
    private final AtomicReference<Thread> runningThread =
            new AtomicReference<>();

    /**
     * 当前任务状态。
     */
    private final AtomicReference<AsyncTaskStatus> status = new AtomicReference<>(AsyncTaskStatus.CREATED);

    /**
     * 最终结果是否已经确定。
     *
     * <p>
     * 用于避免成功、失败、超时、拒绝、取消等多个并发路径重复完成任务。
     * </p>
     */
    private final AtomicBoolean finalized =
            new AtomicBoolean(false);

    public TaskExecutionRuntime(TaskResultMode resultMode) {
        this.resultMode = resultMode == null
                ? TaskResultMode.RESULT_AWARE
                : resultMode;
        this.submitTimeMillis = System.currentTimeMillis();
        this.submitNanoTime = System.nanoTime();
    }

    /**
     * 标记任务已经提交。
     */
    public void markSubmitted() {
        status.set(AsyncTaskStatus.SUBMITTED);
    }

    /**
     * 标记任务开始运行。
     */
    public void markRunning() {
        startTimeMillis = System.currentTimeMillis();
        startNanoTime = System.nanoTime();
        runningThread.set(Thread.currentThread());
        status.set(AsyncTaskStatus.RUNNING);
    }

    /**
     * 尝试进入最终状态。
     *
     * @param terminalStatus 最终状态
     * @return true 表示本次成功确定最终结果；false 表示此前已经被其他路径确定
     */
    public boolean tryFinalize(AsyncTaskStatus terminalStatus) {
        if (!finalized.compareAndSet(false, true)) {
            return false;
        }

        endTimeMillis = System.currentTimeMillis();
        endNanoTime = System.nanoTime();
        status.set(terminalStatus);
        return true;
    }

    /**
     * 记录一个非最终的中间状态。
     *
     * <p>
     * 例如原任务 FAILED 后还会继续执行 fallback，
     * 此时 FAILED 是执行事件，但最终结果尚未确定。
     * </p>
     */
    public void markStatus(AsyncTaskStatus status) {
        if (!finalized.get() && status != null) {
            this.status.set(status);
        }
    }

    /**
     * 清除当前运行线程引用。
     */
    public void clearRunningThread() {
        runningThread.set(null);
    }

    /**
     * 尝试中断当前运行线程。
     *
     * @param interrupt 是否发送中断信号
     */
    public void interruptIfNecessary(boolean interrupt) {
        if (!interrupt) {
            return;
        }

        Thread thread = runningThread.get();

        if (thread != null) {
            thread.interrupt();
        }
    }

    /**
     * 生成不可变的耗时快照。
     */
    public TaskTimingSnapshot timingSnapshot() {
        long nowNano = System.nanoTime();

        long queueCostNanos;

        if (startNanoTime > 0) {
            queueCostNanos = startNanoTime - submitNanoTime;
        } else {
            queueCostNanos = (endNanoTime > 0 ? endNanoTime : nowNano)
                    - submitNanoTime;
        }

        long runCostNanos = 0;

        if (startNanoTime > 0) {
            runCostNanos = (endNanoTime > 0 ? endNanoTime : nowNano)
                    - startNanoTime;
        }

        long totalCostNanos =
                (endNanoTime > 0 ? endNanoTime : nowNano)
                        - submitNanoTime;

        return new TaskTimingSnapshot(
                submitTimeMillis,
                startTimeMillis,
                endTimeMillis,
                nanosToMillis(queueCostNanos),
                nanosToMillis(runCostNanos),
                nanosToMillis(totalCostNanos)
        );
    }

    private long nanosToMillis(long nanos) {
        return Math.max(0L, nanos / 1_000_000L);
    }

    public boolean isResultAware() {
        return resultMode == TaskResultMode.RESULT_AWARE;
    }

    public boolean isFireAndForget() {
        return resultMode == TaskResultMode.FIRE_AND_FORGET;
    }

    public boolean isFinalized() {
        return finalized.get();
    }

    public AsyncTaskStatus getStatus() {
        return status.get();
    }

    public long getSubmitTimeMillis() {
        return submitTimeMillis;
    }
}
