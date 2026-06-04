package com.xjtu.iron.concurrency.core.execution;

import com.xjtu.iron.concurrency.api.context.ContextAwareTaskDecorator;
import com.xjtu.iron.concurrency.api.exception.ConcurrencyRejectedException;
import com.xjtu.iron.concurrency.api.execution.AsyncTask;
import com.xjtu.iron.concurrency.api.execution.AsyncTemplate;
import com.xjtu.iron.concurrency.core.metrics.ConcurrencyMetricsRecorder;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Supplier;

/**
 * 默认任务投递模板。
 *
 * <p>它是并行组件提交任务的核心链路。</p>
 */
public class DefaultTaskExecutionTemplate implements TaskExecutionTemplate {

    private final ThreadPoolRegistry threadPoolRegistry;

    private final ContextAwareTaskDecorator taskDecorator;

    private final AsyncTemplate asyncTemplate;

    private final ConcurrencyMetricsRecorder metricsRecorder;

    public DefaultTaskExecutionTemplate(
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
        validateName(executorName, taskName);

        ThreadPoolExecutor executor = threadPoolRegistry.getExecutor(executorName);
        Runnable decoratedRunnable = taskDecorator.decorate(runnable);
        TaskCommand<Void> command = TaskCommand.fireAndForget(
                executorName,
                taskName,
                decoratedRunnable,
                metricsRecorder
        );

        try {
            metricsRecorder.recordSubmitted(executorName, taskName);
            executor.execute(command);
        } catch (RejectedExecutionException ex) {
            metricsRecorder.recordRejected(executorName, taskName);
            throw new ConcurrencyRejectedException(executorName, taskName, ex);
        }
    }

    @Override
    public boolean tryExecute(String executorName, String taskName, Runnable runnable) {
        try {
            execute(executorName, taskName, runnable);
            return true;
        } catch (ConcurrencyRejectedException ex) {
            return false;
        }
    }

    @Override
    public CompletableFuture<Void> run(String executorName, String taskName, Runnable runnable) {
        Objects.requireNonNull(runnable, "runnable must not be null");
        return supply(executorName, taskName, () -> {
            runnable.run();
            return null;
        });
    }

    @Override
    public <T> CompletableFuture<T> supply(String executorName, String taskName, Supplier<T> supplier) {
        return submit(AsyncTask.of(executorName, taskName, supplier));
    }

    @Override
    public <T> CompletableFuture<T> submit(AsyncTask<T> task) {
        task.validate();

        ThreadPoolExecutor executor = threadPoolRegistry.getExecutor(task.getExecutorName());
        CompletableFuture<T> future = new CompletableFuture<>();

        Supplier<T> supplier = task.isContextPropagation()
                ? taskDecorator.decorate(task.getSupplier())
                : task.getSupplier();

        TaskCommand<T> command = TaskCommand.withFuture(
                task.getExecutorName(),
                task.getTaskName(),
                supplier,
                future,
                metricsRecorder
        );

        try {
            metricsRecorder.recordSubmitted(task.getExecutorName(), task.getTaskName());
            executor.execute(command);
        } catch (RejectedExecutionException ex) {
            metricsRecorder.recordRejected(task.getExecutorName(), task.getTaskName());
            future.completeExceptionally(
                    new ConcurrencyRejectedException(task.getExecutorName(), task.getTaskName(), ex)
            );
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

    private void validateName(String executorName, String taskName) {
        if (executorName == null || executorName.isBlank()) {
            throw new IllegalArgumentException("executorName must not be blank");
        }

        if (taskName == null || taskName.isBlank()) {
            throw new IllegalArgumentException("taskName must not be blank");
        }
    }
}
