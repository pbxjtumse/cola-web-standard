package com.xjtu.iron.concurrency.core.execution;

import com.xjtu.iron.concurrency.api.context.ContextAwareTaskDecorator;
import com.xjtu.iron.concurrency.api.exception.ConcurrencyRejectedException;
import com.xjtu.iron.concurrency.api.execution.AsyncTask;
import com.xjtu.iron.concurrency.api.execution.AsyncTemplate;
import com.xjtu.iron.concurrency.api.listener.AsyncUncaughtExceptionHandler;
import com.xjtu.iron.concurrency.api.listener.TaskExecutionListener;
import com.xjtu.iron.concurrency.core.metrics.ConcurrencyMetricsRecorder;
import com.xjtu.iron.concurrency.core.spi.TaskExecutionTemplate;
import com.xjtu.iron.concurrency.core.spi.ThreadPoolRegistry;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

/**
 * 默认任务投递模板。
 *
 * <p>它是并行组件提交任务的核心链路，统一处理：</p>
 * <ul>
 *     <li>任务校验。</li>
 *     <li>线程池选择。</li>
 *     <li>上下文传播。</li>
 *     <li>任务监听器。</li>
 *     <li>排队耗时、执行耗时、总耗时。</li>
 *     <li>拒绝策略和 Future 状态打通。</li>
 *     <li>timeout / fallback。</li>
 *     <li>fire-and-forget 异常处理。</li>
 * </ul>
 */
public class DefaultTaskExecutionTemplate implements TaskExecutionTemplate {

    /**
     * 线程池注册中心。
     */
    private final ThreadPoolRegistry threadPoolRegistry;

    /**
     * 上下文传播装饰器。
     */
    private final ContextAwareTaskDecorator taskDecorator;

    /**
     * CompletableFuture 编排模板。
     */
    private final AsyncTemplate asyncTemplate;

    /**
     * 指标记录器。
     */
    private final ConcurrencyMetricsRecorder metricsRecorder;

    /**
     * 任务执行监听器列表。
     */
    private final List<TaskExecutionListener> listeners;

    /**
     * fire-and-forget 异常处理器。
     */
    private final AsyncUncaughtExceptionHandler uncaughtExceptionHandler;

    /**
     * 创建默认任务投递模板。
     *
     * @param threadPoolRegistry 线程池注册中心
     * @param taskDecorator 上下文传播装饰器
     * @param asyncTemplate 异步编排模板
     * @param metricsRecorder 指标记录器
     * @param listeners 任务监听器列表
     * @param uncaughtExceptionHandler fire-and-forget 异常处理器
     */
    public DefaultTaskExecutionTemplate(
            ThreadPoolRegistry threadPoolRegistry,
            ContextAwareTaskDecorator taskDecorator,
            AsyncTemplate asyncTemplate,
            ConcurrencyMetricsRecorder metricsRecorder,
            List<TaskExecutionListener> listeners,
            AsyncUncaughtExceptionHandler uncaughtExceptionHandler
    ) {
        this.threadPoolRegistry = threadPoolRegistry;
        this.taskDecorator = taskDecorator;
        this.asyncTemplate = asyncTemplate;
        this.metricsRecorder = metricsRecorder;
        this.listeners = listeners == null ? Collections.emptyList() : listeners;
        this.uncaughtExceptionHandler = uncaughtExceptionHandler;
    }

    /**
     * 兼容旧构造方法。
     *
     * @param threadPoolRegistry 线程池注册中心
     * @param taskDecorator 上下文传播装饰器
     * @param asyncTemplate 异步编排模板
     * @param metricsRecorder 指标记录器
     */
    public DefaultTaskExecutionTemplate(
            ThreadPoolRegistry threadPoolRegistry,
            ContextAwareTaskDecorator taskDecorator,
            AsyncTemplate asyncTemplate,
            ConcurrencyMetricsRecorder metricsRecorder
    ) {
        this(threadPoolRegistry, taskDecorator, asyncTemplate, metricsRecorder, Collections.emptyList(), null);
    }

    @Override
    public void execute(String executorName, String taskName, Runnable runnable) {
        Objects.requireNonNull(runnable, "runnable must not be null");
        AsyncTask<Void> task = AsyncTask.of(executorName, taskName, () -> {
            runnable.run();
            return null;
        });

        task.validate();

        if (task.isContextPropagation()) {
            Supplier<Void> decoratedSupplier = taskDecorator.decorate(task.getSupplier());
            task.setSupplier(decoratedSupplier);
        }

        ThreadPoolExecutor executor = threadPoolRegistry.getExecutor(task.getExecutorName());
        TaskCommand<Void> command = TaskCommand.fireAndForget(task, metricsRecorder, listeners, uncaughtExceptionHandler);

        try {
            command.submitted();
            executor.execute(command);
        } catch (RejectedExecutionException ex) {
            command.reject(ex);
            throw new ConcurrencyRejectedException(task.getExecutorName(), task.getTaskName(), ex);
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
        CompletableFuture<T> baseFuture = new CompletableFuture<>();

        if (task.isContextPropagation()) {
            Supplier<T> supplier = taskDecorator.decorate(task.getSupplier());
            task.setSupplier(supplier);
        }

        TaskCommand<T> command = TaskCommand.withFuture(
                task,
                baseFuture,
                metricsRecorder,
                listeners,
                uncaughtExceptionHandler
        );

        try {
            command.submitted();
            executor.execute(command);
        } catch (RejectedExecutionException ex) {
            command.reject(ex);
        }

        CompletableFuture<T> result = baseFuture;

        if (task.getTimeout() != null) {
            result = asyncTemplate.withTimeout(result, task.getTimeout());
            result.whenComplete((value, error) -> {
                Throwable unwrapped = unwrap(error);
                if (unwrapped instanceof TimeoutException) {
                    command.timeout(unwrapped);
                }
            });
        }

        if (task.getFallback() != null) {
            result = result.exceptionally(error -> {
                Throwable unwrapped = unwrap(error);
                command.fallback(unwrapped);
                return task.getFallback().apply(unwrapped);
            });
        }

        return result;
    }

    private Throwable unwrap(Throwable error) {
        if (error == null) {
            return null;
        }

        if (error instanceof CompletionException && error.getCause() != null) {
            return error.getCause();
        }

        if (error.getCause() != null && !(error instanceof TimeoutException)) {
            return error.getCause();
        }

        return error;
    }
}
