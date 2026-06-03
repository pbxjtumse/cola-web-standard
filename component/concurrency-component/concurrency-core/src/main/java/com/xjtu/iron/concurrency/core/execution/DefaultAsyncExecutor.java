package com.xjtu.iron.concurrency.core.execution;

import com.xjtu.iron.concurrency.api.context.ContextAwareTaskDecorator;
import com.xjtu.iron.concurrency.api.exception.AsyncTaskException;
import com.xjtu.iron.concurrency.api.exception.ConcurrencyRejectedException;
import com.xjtu.iron.concurrency.api.execution.AsyncExecutor;
import com.xjtu.iron.concurrency.api.execution.AsyncTask;
import com.xjtu.iron.concurrency.api.execution.AsyncTemplate;
import com.xjtu.iron.concurrency.core.metrics.ConcurrencyMetricsRecorder;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Supplier;

public class DefaultAsyncExecutor implements AsyncExecutor {

    private final ThreadPoolRegistry threadPoolRegistry;

    private final ContextAwareTaskDecorator taskDecorator;

    private final AsyncTemplate asyncTemplate;

    private final ConcurrencyMetricsRecorder metricsRecorder;

    public DefaultAsyncExecutor(
            ThreadPoolRegistry threadPoolRegistry,
            ContextAwareTaskDecorator taskDecorator,
            AsyncTemplate asyncTemplate,
            ConcurrencyMetricsRecorder metricsRecorder
    ) {
        this.threadPoolRegistry = threadPoolRegistry;
        this.taskDecorator = taskDecorator;
        this.asyncTemplate = asyncTemplate;
        this.metricsRecorder = metricsRecorder;
    }

    @Override
    public void execute(String executorName, String taskName, Runnable runnable) {
        Objects.requireNonNull(runnable, "runnable must not be null");
        ExecutorService executor = threadPoolRegistry.getExecutor(executorName);
        Runnable command = () -> {
            long start = System.currentTimeMillis();

            try {
                runnable.run();
                metricsRecorder.recordSuccess(executorName, taskName, System.currentTimeMillis() - start);
            } catch (Throwable ex) {
                metricsRecorder.recordFailure(executorName, taskName, System.currentTimeMillis() - start, ex);
                throw new AsyncTaskException(executorName, taskName, ex);
            }
        };
        command = taskDecorator.decorate(command);
        try {
            metricsRecorder.recordSubmitted(executorName, taskName);
            executor.execute(command);
        } catch (RejectedExecutionException ex) {
            metricsRecorder.recordRejected(executorName, taskName);
            throw new ConcurrencyRejectedException(executorName, taskName, ex);
        }
    }

    @Override
    public CompletableFuture<Void> run(String executorName, String taskName, Runnable runnable) {
        return supply(executorName, taskName, () -> {runnable.run();return null;});
    }

    @Override
    public <T> CompletableFuture<T> supply(String executorName, String taskName, Supplier<T> supplier) {
        return submit(AsyncTask.of(executorName, taskName, supplier));
    }

    @Override
    public <T> CompletableFuture<T> submit(AsyncTask<T> task) {
        task.validate();
        ExecutorService executor = threadPoolRegistry.getExecutor(task.getExecutorName());
        CompletableFuture<T> future = new CompletableFuture<>();
        Supplier<T> supplier = task.isContextPropagation()
                ? taskDecorator.decorate(task.getSupplier())
                : task.getSupplier();

        Runnable command = () -> {
            long start = System.currentTimeMillis();

            try {
                T value = supplier.get();
                metricsRecorder.recordSuccess(task.getExecutorName(), task.getTaskName(),
                        System.currentTimeMillis() - start);
                future.complete(value);
            } catch (Throwable ex) {
                AsyncTaskException taskException = new AsyncTaskException(task.getExecutorName(), task.getTaskName(), ex);
                metricsRecorder.recordFailure(task.getExecutorName(), task.getTaskName(),
                        System.currentTimeMillis() - start, ex);
                future.completeExceptionally(taskException);
            }
        };

        try {
            metricsRecorder.recordSubmitted(task.getExecutorName(), task.getTaskName());
            executor.execute(command);
        } catch (RejectedExecutionException ex) {
            ConcurrencyRejectedException rejectedException =
                    new ConcurrencyRejectedException(task.getExecutorName(), task.getTaskName(), ex);

            metricsRecorder.recordRejected(task.getExecutorName(), task.getTaskName());

            future.completeExceptionally(rejectedException);
        }

        CompletableFuture<T> result = future;

        if (task.getTimeout() != null) {
            result = asyncTemplate.withTimeout(result, task.getTimeout());
        }

        if (task.getFallback() != null) {
            result = asyncTemplate.withFallback(result, task.getFallback());
        }

        return result;
    }
}