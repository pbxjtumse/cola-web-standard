package com.xjtu.iron.concurrency.core.execution;

import com.xjtu.iron.concurrency.api.context.ContextAwareTaskDecorator;
import com.xjtu.iron.concurrency.api.enums.AsyncTaskStatus;
import com.xjtu.iron.concurrency.api.exception.ConcurrencyRejectedException;
import com.xjtu.iron.concurrency.api.execution.AsyncTask;
import com.xjtu.iron.concurrency.api.execution.AsyncTemplate;
import com.xjtu.iron.concurrency.api.execution.TaskExecutionRegistry;
import com.xjtu.iron.concurrency.api.listener.AsyncUncaughtExceptionHandler;
import com.xjtu.iron.concurrency.api.listener.TaskExecutionEvent;
import com.xjtu.iron.concurrency.api.listener.TaskExecutionListener;
import com.xjtu.iron.concurrency.core.metrics.ConcurrencyMetricsRecorder;
import com.xjtu.iron.concurrency.core.spi.TaskExecutionTemplate;
import com.xjtu.iron.concurrency.core.spi.ThreadPoolRegistry;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Supplier;

/**
 * 默认任务投递模板。
 */
public class DefaultTaskExecutionTemplate implements TaskExecutionTemplate {

    private final ThreadPoolRegistry threadPoolRegistry;
    private final ContextAwareTaskDecorator taskDecorator;
    private final AsyncTemplate asyncTemplate;
    private final ConcurrencyMetricsRecorder metricsRecorder;
    private final List<TaskExecutionListener> listeners;
    private final AsyncUncaughtExceptionHandler uncaughtExceptionHandler;
    private final TaskExecutionRegistry taskExecutionRegistry;

    public DefaultTaskExecutionTemplate(
            ThreadPoolRegistry threadPoolRegistry,
            ContextAwareTaskDecorator taskDecorator,
            AsyncTemplate asyncTemplate,
            ConcurrencyMetricsRecorder metricsRecorder,
            List<TaskExecutionListener> listeners,
            AsyncUncaughtExceptionHandler uncaughtExceptionHandler,
            TaskExecutionRegistry taskExecutionRegistry
    ) {
        this.threadPoolRegistry = threadPoolRegistry;
        this.taskDecorator = taskDecorator;
        this.asyncTemplate = asyncTemplate;
        this.metricsRecorder = metricsRecorder;
        this.listeners = listeners == null ? Collections.emptyList() : List.copyOf(listeners);
        this.uncaughtExceptionHandler = uncaughtExceptionHandler;
        this.taskExecutionRegistry = taskExecutionRegistry;
    }

    @Override
    public void execute(String executorName, String taskName, Runnable runnable) {
        Objects.requireNonNull(runnable, "runnable must not be null");
        AsyncTask<Void> task = AsyncTask.of(executorName, taskName, () -> {
            runnable.run();
            return null;
        });
        submitInternal(task, true);
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
        return submit(AsyncTask.of(executorName, taskName, () -> {
            runnable.run();
            return null;
        }));
    }

    @Override
    public <T> CompletableFuture<T> supply(String executorName, String taskName, Supplier<T> supplier) {
        return submit(AsyncTask.of(executorName, taskName, supplier));
    }

    @Override
    public <T> CompletableFuture<T> submit(AsyncTask<T> task) {
        return submitInternal(task, false);
    }

    private <T> CompletableFuture<T> submitInternal(AsyncTask<T> task, boolean fireAndForget) {
        task.validate();

        ThreadPoolExecutor executor = threadPoolRegistry.getExecutor(task.getExecutorName());
        CompletableFuture<T> baseFuture = new CompletableFuture<>();

        Supplier<T> supplier = task.isContextPropagation()
                ? taskDecorator.decorate(task.getSupplier())
                : task.getSupplier();

        TaskCommand<T> command = new TaskCommand<>(
                task,
                supplier,
                baseFuture,
                metricsRecorder,
                listeners,
                uncaughtExceptionHandler,
                taskExecutionRegistry,
                fireAndForget
        );

        TaskExecutionEvent submitted = buildSubmittedEvent(task, fireAndForget);
        metricsRecorder.recordSubmitted(submitted.copy());
        listeners.forEach(listener -> listener.onSubmitted(submitted.copy()));

        try {
            executor.execute(command);
        } catch (RejectedExecutionException ex) {
            command.reject(ex);
            if (fireAndForget) {
                throw new ConcurrencyRejectedException(task.getExecutorName(), task.getTaskName(), ex);
            }
        }

        if (fireAndForget) {
            return baseFuture;
        }

        CompletableFuture<T> result = baseFuture;
        if (task.getTimeout() != null) {
            result = asyncTemplate.withTimeout(result, task.getTimeout());
            result = result.whenComplete((value, error) -> {
                if (error != null && isTimeout(error)) {
                    TaskExecutionEvent timeout = buildTimeoutEvent(task, error);
                    metricsRecorder.recordTimeout(timeout.copy());
                    listeners.forEach(listener -> listener.onTimeout(timeout.copy()));
                    if (task.isCancelOnTimeout()) {
                        command.cancelRunning(task.isInterruptOnCancel());
                    }
                }
            });
        }

        if (task.getFallback() != null) {
            CompletableFuture<T> beforeFallback = result;
            result = asyncTemplate.withFallback(beforeFallback, error -> {
                TaskExecutionEvent fallback = buildFallbackEvent(task, error);
                metricsRecorder.recordFallback(fallback.copy());
                listeners.forEach(listener -> listener.onFallback(fallback.copy()));
                return task.getFallback().apply(error);
            });
        }

        return result;
    }

    private TaskExecutionEvent buildSubmittedEvent(AsyncTask<?> task, boolean fireAndForget) {
        TaskExecutionEvent event = new TaskExecutionEvent();
        event.setTaskId(task.getTaskId());
        event.setExecutorName(task.getExecutorName());
        event.setTaskName(task.getTaskName());
        event.setBizKey(task.getBizKey());
        event.setDescription(task.getDescription());
        event.setTags(task.getTags());
        event.setStatus(AsyncTaskStatus.SUBMITTED);
        event.setSubmitTimeMillis(System.currentTimeMillis());
        event.setFireAndForget(fireAndForget);
        return event;
    }

    private TaskExecutionEvent buildTimeoutEvent(AsyncTask<?> task, Throwable throwable) {
        TaskExecutionEvent event = buildSubmittedEvent(task, false);
        long now = System.currentTimeMillis();
        event.setStatus(AsyncTaskStatus.TIMEOUT);
        event.setEndTimeMillis(now);
        event.setThrowable(throwable);
        return event;
    }

    private TaskExecutionEvent buildFallbackEvent(AsyncTask<?> task, Throwable throwable) {
        TaskExecutionEvent event = buildSubmittedEvent(task, false);
        long now = System.currentTimeMillis();
        event.setStatus(AsyncTaskStatus.FALLBACK);
        event.setEndTimeMillis(now);
        event.setThrowable(throwable);
        return event;
    }

    private boolean isTimeout(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof java.util.concurrent.TimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
