package com.xjtu.iron.concurrency.core.task;

import com.xjtu.iron.concurrency.api.enums.task.AsyncTaskStatus;
import com.xjtu.iron.concurrency.api.task.TaskResultMode;
import com.xjtu.iron.concurrency.api.task.TaskTimingSnapshot;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 任务执行运行时状态。
 *
 * <p>
 * 该对象只在 concurrency-core 内部使用，保存线程、单调时钟和并发终态控制等不可序列化状态。
 * TaskExecutionEvent 与 TaskExecutionSnapshot 只接收它生成的只读时间快照。
 * </p>
 */
public final class TaskExecutionRuntime {

    /**
     * 任务结果模式。
     */
    private final TaskResultMode resultMode;

    /**
     * 任务提交时的墙上时钟时间，供日志和页面展示。
     */
    private final long submitTimeMillis;

    /**
     * 任务提交时的单调时钟值，专门用于可靠计算耗时。
     */
    private final long submitNanoTime;

    /**
     * 原始任务真正开始执行时的墙上时钟时间。
     */
    private volatile long startTimeMillis;

    /**
     * 原始任务真正开始执行时的单调时钟值。
     */
    private volatile long startNanoTime;

    /**
     * 原始任务结果确定时的墙上时钟时间。
     *
     * <p>例如原始 Supplier 成功、失败、被拒绝、超时或取消。</p>
     */
    private volatile long executionEndTimeMillis;

    /**
     * 原始任务结果确定时的单调时钟值。
     */
    private volatile long executionEndNanoTime;

    /**
     * 整个任务管道最终完成时的墙上时钟时间。
     *
     * <p>配置 fallback 时，该时间可能晚于原始任务结束时间。</p>
     */
    private volatile long finalEndTimeMillis;

    /**
     * 整个任务管道最终完成时的单调时钟值。
     */
    private volatile long finalEndNanoTime;

    /**
     * 当前正在执行原始任务的线程。
     *
     * <p>用于超时或取消后尽力发送 interrupt；Java 中断不是强制终止。</p>
     */
    private final AtomicReference<Thread> runningThread = new AtomicReference<>();

    /**
     * 当前最新任务状态。
     */
    private final AtomicReference<AsyncTaskStatus> status =
            new AtomicReference<>(AsyncTaskStatus.CREATED);

    /**
     * 原始任务结果是否已经确定。
     *
     * <p>
     * 用于解决结果超时与工作线程晚到成功之间的竞争，确保原始任务只记录一个结果。
     * </p>
     */
    private final AtomicBoolean baseOutcomeResolved = new AtomicBoolean(false);

    /**
     * 整个结果管道是否已经进入最终状态。
     *
     * <p>
     * 原始任务失败但存在 fallback 时，baseOutcomeResolved 已经为 true，
     * finalOutcomeResolved 要等到 FALLBACK_SUCCESS 或 FALLBACK_FAILED 后才变为 true。
     * </p>
     */
    private final AtomicBoolean finalOutcomeResolved = new AtomicBoolean(false);

    public TaskExecutionRuntime(TaskResultMode resultMode) {
        this.resultMode = resultMode == null ? TaskResultMode.RESULT_AWARE : resultMode;
        this.submitTimeMillis = System.currentTimeMillis();
        this.submitNanoTime = System.nanoTime();
    }

    /**
     * 标记任务已提交。
     */
    public void markSubmitted() {
        status.set(AsyncTaskStatus.SUBMITTED);
    }

    /**
     * 标记原始任务开始运行。
     */
    public void markRunning() {
        startTimeMillis = System.currentTimeMillis();
        startNanoTime = System.nanoTime();
        runningThread.set(Thread.currentThread());
        status.set(AsyncTaskStatus.RUNNING);
    }

    /**
     * 尝试确定原始任务结果。
     *
     * @param baseStatus 原始任务结果状态
     * @return true 表示当前路径首次确定原始任务结果
     */
    public boolean tryResolveBaseOutcome(AsyncTaskStatus baseStatus) {
        Objects.requireNonNull(baseStatus, "baseStatus must not be null");

        if (!baseOutcomeResolved.compareAndSet(false, true)) {
            return false;
        }

        executionEndTimeMillis = System.currentTimeMillis();
        executionEndNanoTime = System.nanoTime();
        status.set(baseStatus);
        return true;
    }

    /**
     * 记录 fallback 等非最终过程状态。
     *
     * @param intermediateStatus 中间状态
     */
    public void markIntermediate(AsyncTaskStatus intermediateStatus) {
        if (!finalOutcomeResolved.get() && intermediateStatus != null) {
            status.set(intermediateStatus);
        }
    }

    /**
     * 尝试确定整个任务管道的最终状态。
     *
     * @param finalStatus 最终状态
     * @return true 表示当前路径首次确定最终状态
     */
    public boolean tryFinalize(AsyncTaskStatus finalStatus) {
        Objects.requireNonNull(finalStatus, "finalStatus must not be null");

        if (!finalOutcomeResolved.compareAndSet(false, true)) {
            return false;
        }

        finalEndTimeMillis = System.currentTimeMillis();
        finalEndNanoTime = System.nanoTime();
        status.set(finalStatus);
        return true;
    }

    /**
     * 判断任务是否已经发生排队超时。
     *
     * @param queueTimeout 排队超时时间
     * @return 是否超时
     */
    public boolean isQueueTimeout(Duration queueTimeout) {
        if (queueTimeout == null || queueTimeout.isZero() || queueTimeout.isNegative()) {
            return false;
        }
        return elapsedMillis(submitNanoTime, System.nanoTime()) > queueTimeout.toMillis();
    }

    /**
     * 清除当前运行线程引用，避免线程对象被长期持有。
     */
    public void clearRunningThread() {
        runningThread.set(null);
    }

    /**
     * 尽力中断当前运行线程。
     *
     * @param mayInterruptIfRunning 是否发送中断信号
     */
    public void interruptIfNecessary(boolean mayInterruptIfRunning) {
        if (!mayInterruptIfRunning) {
            return;
        }

        Thread thread = runningThread.get();
        if (thread != null) {
            thread.interrupt();
        }
    }

    /**
     * 生成当前时间与耗时快照。
     *
     * <p>
     * queueCost 表示提交到开始运行的时间；runCost 只统计原始任务执行时间；
     * totalCost 在有 fallback 时会包含 fallback 的处理时间。
     * </p>
     *
     * @return 不可变时间快照
     */
    public TaskTimingSnapshot timingSnapshot() {
        long nowNano = System.nanoTime();
        long nowMillis = System.currentTimeMillis();
        boolean fallbackInProgress = !finalOutcomeResolved.get()
                && status.get() == AsyncTaskStatus.FALLBACK;
        long visibleEndMillis = finalEndTimeMillis > 0
                ? finalEndTimeMillis
                : (fallbackInProgress ? nowMillis : executionEndTimeMillis);
        long visibleEndNano = finalEndNanoTime > 0
                ? finalEndNanoTime
                : (fallbackInProgress ? nowNano : executionEndNanoTime);

        long queueCostNanos = startNanoTime > 0
                ? startNanoTime - submitNanoTime
                : (visibleEndNano > 0 ? visibleEndNano : nowNano) - submitNanoTime;
        long runCostNanos = startNanoTime > 0
                ? (executionEndNanoTime > 0 ? executionEndNanoTime : nowNano) - startNanoTime
                : 0L;
        long totalCostNanos = (visibleEndNano > 0 ? visibleEndNano : nowNano) - submitNanoTime;

        return new TaskTimingSnapshot(
                submitTimeMillis,
                startTimeMillis,
                visibleEndMillis,
                nanosToMillis(queueCostNanos),
                nanosToMillis(runCostNanos),
                nanosToMillis(totalCostNanos)
        );
    }

    /**
     * 判断调用方是否需要感知结果。
     */
    public boolean isResultAware() {
        return resultMode == TaskResultMode.RESULT_AWARE;
    }

    /**
     * 判断任务是否为只投递模式。
     */
    public boolean isFireAndForget() {
        return resultMode == TaskResultMode.FIRE_AND_FORGET;
    }

    public TaskResultMode getResultMode() {
        return resultMode;
    }

    public AsyncTaskStatus getStatus() {
        return status.get();
    }

    public boolean isBaseOutcomeResolved() {
        return baseOutcomeResolved.get();
    }

    public boolean isFinalOutcomeResolved() {
        return finalOutcomeResolved.get();
    }

    private long elapsedMillis(long startNanos, long endNanos) {
        return nanosToMillis(Math.max(0L, endNanos - startNanos));
    }

    private long nanosToMillis(long nanos) {
        return Math.max(0L, nanos / 1_000_000L);
    }
}
