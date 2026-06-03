package com.xjtu.iron.concurrency.core.execution;

import com.xjtu.iron.concurrency.api.context.ContextAwareTaskDecorator;
import com.xjtu.iron.concurrency.api.execution.AsyncExecutor;
import com.xjtu.iron.concurrency.api.execution.AsyncTask;
import com.xjtu.iron.concurrency.api.execution.AsyncTemplate;
import com.xjtu.iron.concurrency.core.metrics.ConcurrencyMetricsRecorder;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

/**
 * 默认异步执行器。
 */
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
    public <T> CompletableFuture<T> supply(
            String executorName,
            String taskName,
            Supplier<T> supplier
    ) {
        return submit(AsyncTask.of(executorName, taskName, supplier));
    }

    @Override
    public CompletableFuture<Void> run(
            String executorName,
            String taskName,
            Runnable runnable
    ) {
        return submit(AsyncTask.of(executorName, taskName, () -> {
            runnable.run();
            return null;
        }));
    }

    @Override
    public <T> CompletableFuture<T> submit(AsyncTask<T> task) {
        task.validate();

        ExecutorService executor = threadPoolRegistry.getExecutor(task.getExecutorName());

        Supplier<T> supplier = task.isContextPropagation()
                ? taskDecorator.decorate(task.getSupplier())
                : task.getSupplier();

        CompletableFuture<T> future = CompletableFuture.supplyAsync(() -> {
            long start = System.currentTimeMillis();

            try {
                metricsRecorder.recordSubmitted(task.getExecutorName(), task.getTaskName());

                T value = supplier.get();

                metricsRecorder.recordSuccess(
                        task.getExecutorName(),
                        task.getTaskName(),
                        System.currentTimeMillis() - start
                );

                return value;
            } catch (Throwable ex) {
                metricsRecorder.recordFailure(
                        task.getExecutorName(),
                        task.getTaskName(),
                        System.currentTimeMillis() - start,
                        ex
                );

                throw ex;
            }
        }, executor);

        if (task.getTimeout() != null) {
            future = asyncTemplate.withTimeout(future, task.getTimeout());
        }

        if (task.getFallback() != null) {
            future = asyncTemplate.withFallback(future, task.getFallback());
        }

        return future;
    }
}