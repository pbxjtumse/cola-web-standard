package com.xjtu.iron.concurrency.core.execution;

import com.xjtu.iron.concurrency.api.exception.AsyncTaskException;
import com.xjtu.iron.concurrency.api.exception.ConcurrencyRejectedException;
import com.xjtu.iron.concurrency.core.metrics.ConcurrencyMetricsRecorder;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * 线程池真正执行的任务命令。
 *
 * <p>它把业务 Supplier/Runnable、CompletableFuture、指标、异常包装统一收口。</p>
 */
public class TaskCommand<T> implements Runnable, RejectedTaskAware {

    private final String executorName;
    private final String taskName;
    private final Supplier<T> supplier;
    private final CompletableFuture<T> future;
    private final ConcurrencyMetricsRecorder metricsRecorder;

    public TaskCommand(
            String executorName,
            String taskName,
            Supplier<T> supplier,
            CompletableFuture<T> future,
            ConcurrencyMetricsRecorder metricsRecorder
    ) {
        this.executorName = executorName;
        this.taskName = taskName;
        this.supplier = supplier;
        this.future = future;
        this.metricsRecorder = metricsRecorder;
    }

    public static TaskCommand<Void> fireAndForget(
            String executorName,
            String taskName,
            Runnable runnable,
            ConcurrencyMetricsRecorder metricsRecorder
    ) {
        return new TaskCommand<>(
                executorName,
                taskName,
                () -> {
                    runnable.run();
                    return null;
                },
                null,
                metricsRecorder
        );
    }

    public static <T> TaskCommand<T> withFuture(
            String executorName,
            String taskName,
            Supplier<T> supplier,
            CompletableFuture<T> future,
            ConcurrencyMetricsRecorder metricsRecorder
    ) {
        return new TaskCommand<>(executorName, taskName, supplier, future, metricsRecorder);
    }

    @Override
    public void run() {
        long start = System.currentTimeMillis();

        try {
            T value = supplier.get();
            metricsRecorder.recordSuccess(executorName, taskName, System.currentTimeMillis() - start);

            if (future != null) {
                future.complete(value);
            }
        } catch (Throwable ex) {
            AsyncTaskException taskException = new AsyncTaskException(executorName, taskName, ex);
            metricsRecorder.recordFailure(executorName, taskName, System.currentTimeMillis() - start, ex);

            if (future != null) {
                future.completeExceptionally(taskException);
                return;
            }

            throw taskException;
        }
    }

    @Override
    public void reject(Throwable ex) {
        metricsRecorder.recordRejected(executorName, taskName);

        if (future != null) {
            future.completeExceptionally(new ConcurrencyRejectedException(executorName, taskName, ex));
        }
    }

    public String getExecutorName() {
        return executorName;
    }

    public String getTaskName() {
        return taskName;
    }
}
