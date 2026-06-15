package com.xjtu.iron.concurrency.core.task;

import com.xjtu.iron.concurrency.api.context.ContextAwareTaskDecorator;
import com.xjtu.iron.concurrency.api.enums.error.AsyncErrorCategory;
import com.xjtu.iron.concurrency.api.enums.error.AsyncErrorReason;
import com.xjtu.iron.concurrency.api.enums.error.AsyncErrorStage;
import com.xjtu.iron.concurrency.api.enums.error.AsyncRecoveryAction;
import com.xjtu.iron.concurrency.api.enums.task.AsyncTaskStatus;
import com.xjtu.iron.concurrency.api.error.ApplicationErrorInfo;
import com.xjtu.iron.concurrency.api.error.AsyncError;
import com.xjtu.iron.concurrency.api.error.AsyncErrorClassification;
import com.xjtu.iron.concurrency.api.error.AsyncErrorClassifier;
import com.xjtu.iron.concurrency.api.error.CompletableFutureExceptionUtils;
import com.xjtu.iron.concurrency.api.error.ExceptionInfo;
import com.xjtu.iron.concurrency.api.error.RecoveryHint;
import com.xjtu.iron.concurrency.api.event.TaskExecutionEvent;
import com.xjtu.iron.concurrency.api.exception.ConcurrencyException;
import com.xjtu.iron.concurrency.api.exception.ConcurrencyRejectedException;
import com.xjtu.iron.concurrency.api.execution.registry.TaskExecutionRegistry;
import com.xjtu.iron.concurrency.api.execution.registry.TaskExecutionSnapshot;
import com.xjtu.iron.concurrency.api.execution.task.AsyncTask;
import com.xjtu.iron.concurrency.api.execution.template.AsyncTemplate;
import com.xjtu.iron.concurrency.api.listener.AsyncUncaughtExceptionHandler;
import com.xjtu.iron.concurrency.api.listener.TaskExecutionListener;
import com.xjtu.iron.concurrency.core.metrics.ConcurrencyMetricsRecorder;
import com.xjtu.iron.concurrency.core.spi.TaskExecutionTemplate;
import com.xjtu.iron.concurrency.core.spi.ThreadPoolRegistry;

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
    private final TaskExecutionListener taskExecutionListener;
    private final AsyncUncaughtExceptionHandler uncaughtExceptionHandler;
    private final TaskExecutionRegistry taskExecutionRegistry;
    private final AsyncErrorClassifier asyncErrorClassifier;

    public DefaultTaskExecutionTemplate(
            ThreadPoolRegistry threadPoolRegistry,
            ContextAwareTaskDecorator taskDecorator,
            AsyncTemplate asyncTemplate,
            ConcurrencyMetricsRecorder metricsRecorder,
            TaskExecutionListener taskExecutionListener,
            AsyncUncaughtExceptionHandler uncaughtExceptionHandler,
            TaskExecutionRegistry taskExecutionRegistry,
            AsyncErrorClassifier asyncErrorClassifier
    ) {
        this.threadPoolRegistry = threadPoolRegistry;
        this.taskDecorator = taskDecorator;
        this.asyncTemplate = asyncTemplate;
        this.metricsRecorder = metricsRecorder;
        this.taskExecutionListener = taskExecutionListener;
        this.uncaughtExceptionHandler = uncaughtExceptionHandler;
        this.taskExecutionRegistry = taskExecutionRegistry;
        this.asyncErrorClassifier = asyncErrorClassifier;
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
                ? taskDecorator.decorate(task.getOperation())
                : task.getOperation();

        TaskCommand<T> command = new TaskCommand<>(
                task,
                supplier,
                baseFuture,
                metricsRecorder,
                taskExecutionListener,
                uncaughtExceptionHandler,
                taskExecutionRegistry,
                asyncErrorClassifier,
                fireAndForget
        );

        TaskExecutionEvent submitted = buildEvent(task, fireAndForget, AsyncTaskStatus.SUBMITTED, AsyncError.none());
        metricsRecorder.recordSubmitted(submitted.copy());
        taskExecutionRegistry.update(TaskExecutionSnapshot.from(submitted));
        taskExecutionListener.onSubmitted(submitted.copy());

        try {
            executor.execute(command);
        } catch (RejectedExecutionException ex) {
            command.reject(ex);
            if (fireAndForget) {
                throw new ConcurrencyRejectedException(task.getExecutorName(), task.getTaskName(), rejectedError(ex), ex);
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
                    command.completeTimeout(error, AsyncErrorStage.WAIT_RESULT);
                    if (task.isCancelOnTimeout()) {
                        command.interruptRunningIfNecessary(task.isInterruptOnCancel());
                    }
                }
            });
        }

        if (task.getFallback() != null) {
            CompletableFuture<T> beforeFallback = result;
            result = asyncTemplate.withFallback(beforeFallback, error -> {
                AsyncError originalError = resolveError(task, error, AsyncErrorStage.FALLBACK);
                publishFallback(task, AsyncTaskStatus.FALLBACK, originalError, error, "Fallback triggered");
                try {
                    T fallbackValue = task.getFallback().apply(error);
                    publishFallback(task, AsyncTaskStatus.FALLBACK_SUCCESS, originalError, error, "Fallback success");
                    return fallbackValue;
                } catch (Throwable fallbackThrowable) {
                    AsyncError fallbackError = fallbackError(fallbackThrowable);
                    publishFallback(task, AsyncTaskStatus.FALLBACK_FAILED, fallbackError, fallbackThrowable, "Fallback failed");
                    throw fallbackThrowable instanceof RuntimeException runtimeException
                            ? runtimeException
                            : new RuntimeException(fallbackThrowable);
                }
            });
        }

        return result;
    }

    private void publishFallback(
            AsyncTask<?> task,
            AsyncTaskStatus status,
            AsyncError error,
            Throwable throwable,
            String message
    ) {
        TaskExecutionEvent event = buildEvent(task, false, status, error);
        long now = System.currentTimeMillis();
        event.setEndTimeMillis(now);
        event.setTotalCostMillis(Math.max(0, now - event.getSubmitTimeMillis()));
        event.setMessage(message);
        metricsRecorder.recordFallback(event.copy());
        taskExecutionRegistry.update(TaskExecutionSnapshot.from(event));
        taskExecutionListener.onFallback(event.copy());
    }

    private TaskExecutionEvent buildEvent(
            AsyncTask<?> task,
            boolean fireAndForget,
            AsyncTaskStatus status,
            AsyncError error
    ) {
        TaskExecutionEvent event = new TaskExecutionEvent();
        event.setTaskId(task.getTaskId());
        event.setExecutorName(task.getExecutorName());
        event.setTaskName(task.getTaskName());
        event.setBizKey(task.getBizKey());
        event.setDescription(task.getDescription());
        event.setTags(task.getTags());
        event.setStatus(status);
        event.setError(error == null ? AsyncError.none() : error);
        event.setSubmitTimeMillis(System.currentTimeMillis());
        event.setFireAndForget(fireAndForget);
        return event;
    }

    private AsyncError resolveError(AsyncTask<?> task, Throwable throwable, AsyncErrorStage stage) {
        Throwable unwrapped = CompletableFutureExceptionUtils.unwrap(throwable);
        if (unwrapped instanceof ConcurrencyException concurrencyException) {
            return concurrencyException.getError() == null ? AsyncError.unknown(throwable) : concurrencyException.getError().copy();
        }
        return asyncErrorClassifier.classify(task, throwable, stage);
    }

    private AsyncError rejectedError(Throwable throwable) {
        return AsyncError.builder()
                .classification(AsyncErrorClassification.of(
                        AsyncErrorCategory.RESOURCE,
                        AsyncErrorReason.REJECTED,
                        AsyncErrorStage.SUBMIT
                ))
                .application(ApplicationErrorInfo.none())
                .exception(ExceptionInfo.from(throwable))
                .recovery(RecoveryHint.of(AsyncRecoveryAction.DELAY_RETRY, true, false, true))
                .build();
    }

    private AsyncError fallbackError(Throwable throwable) {
        return AsyncError.builder()
                .classification(AsyncErrorClassification.of(
                        AsyncErrorCategory.SYSTEM,
                        AsyncErrorReason.FALLBACK_THROWN,
                        AsyncErrorStage.FALLBACK
                ))
                .application(ApplicationErrorInfo.none())
                .exception(ExceptionInfo.from(throwable))
                .recovery(RecoveryHint.of(AsyncRecoveryAction.ALERT, false, false, true))
                .build();
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
