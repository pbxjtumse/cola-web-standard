package com.xjtu.iron.concurrency.core.task;

import com.xjtu.iron.concurrency.api.context.ContextAwareTaskDecorator;
import com.xjtu.iron.concurrency.api.enums.error.AsyncErrorStage;
import com.xjtu.iron.concurrency.api.error.AsyncError;
import com.xjtu.iron.concurrency.api.error.AsyncErrorClassifier;
import com.xjtu.iron.concurrency.api.exception.ConcurrencyRejectedException;
import com.xjtu.iron.concurrency.api.execution.task.AsyncTask;
import com.xjtu.iron.concurrency.api.task.TaskResultMode;
import com.xjtu.iron.concurrency.core.lifecycle.TaskLifecyclePublisher;
import com.xjtu.iron.concurrency.core.pipeline.TaskResultPipeline;
import com.xjtu.iron.concurrency.core.spi.TaskExecutionTemplate;
import com.xjtu.iron.concurrency.core.spi.ThreadPoolRegistry;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Supplier;

/**
 * 默认任务投递模板。
 *
 * <p>
 * 该类只负责校验任务、选择线程池、创建执行上下文、创建 TaskCommand、提交任务，
 * timeout 与 fallback 等结果处理交给 TaskResultPipeline。
 * </p>
 */
public final class DefaultTaskExecutionTemplate implements TaskExecutionTemplate {

    /**
     * 线程池注册表。
     */
    private final ThreadPoolRegistry threadPoolRegistry;

    /**
     * 上下文任务装饰器。
     */
    private final ContextAwareTaskDecorator taskDecorator;

    /**
     * 任务生命周期发布器。
     */
    private final TaskLifecyclePublisher lifecyclePublisher;

    /**
     * timeout 与 fallback 结果管道。
     */
    private final TaskResultPipeline resultPipeline;

    /**
     * 组合后的错误分类器。
     */
    private final AsyncErrorClassifier errorClassifier;

    /**
     * fire-and-forget 异常处理器。
     */
    private final com.xjtu.iron.concurrency.api.listener.AsyncUncaughtExceptionHandler uncaughtExceptionHandler;

    public DefaultTaskExecutionTemplate(
            ThreadPoolRegistry threadPoolRegistry,
            ContextAwareTaskDecorator taskDecorator,
            TaskLifecyclePublisher lifecyclePublisher,
            TaskResultPipeline resultPipeline,
            AsyncErrorClassifier errorClassifier,
            com.xjtu.iron.concurrency.api.listener.AsyncUncaughtExceptionHandler uncaughtExceptionHandler
    ) {
        this.threadPoolRegistry = Objects.requireNonNull(threadPoolRegistry, "threadPoolRegistry must not be null");
        this.taskDecorator = Objects.requireNonNull(taskDecorator, "taskDecorator must not be null");
        this.lifecyclePublisher = Objects.requireNonNull(lifecyclePublisher, "lifecyclePublisher must not be null");
        this.resultPipeline = Objects.requireNonNull(resultPipeline, "resultPipeline must not be null");
        this.errorClassifier = Objects.requireNonNull(errorClassifier, "errorClassifier must not be null");
        this.uncaughtExceptionHandler = Objects.requireNonNull(
                uncaughtExceptionHandler,
                "uncaughtExceptionHandler must not be null"
        );
    }

    @Override
    public void execute(String executorName, String taskName, Runnable runnable) {
        Objects.requireNonNull(runnable, "runnable must not be null");
        AsyncTask<Void> task = AsyncTask.of(executorName, taskName, () -> {
            runnable.run();
            return null;
        });
        submitInternal(task, TaskResultMode.FIRE_AND_FORGET);
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
    public <T> CompletableFuture<T> supply(
            String executorName,
            String taskName,
            Supplier<T> supplier
    ) {
        return submit(AsyncTask.of(executorName, taskName, supplier));
    }

    @Override
    public <T> CompletableFuture<T> submit(AsyncTask<T> task) {
        return submitInternal(task, TaskResultMode.RESULT_AWARE);
    }

    /**
     * 执行统一任务提交流程。
     */
    private <T> CompletableFuture<T> submitInternal(
            AsyncTask<T> task,
            TaskResultMode resultMode
    ) {
        Objects.requireNonNull(task, "task must not be null");
        task.validate();

        ThreadPoolExecutor executor = threadPoolRegistry.getExecutor(task.getExecutorName());
        CompletableFuture<T> baseFuture = new CompletableFuture<>();
        Supplier<T> executable = task.isContextPropagation()
                ? taskDecorator.decorate(task.getOperation())
                : task.getOperation();
        TaskExecutionRuntime runtime = new TaskExecutionRuntime(resultMode);
        TaskExecutionContext<T> context = new TaskExecutionContext<>(
                task,
                executable,
                baseFuture,
                runtime
        );
        TaskCommand<T> command = new TaskCommand<>(
                context,
                lifecyclePublisher,
                errorClassifier,
                uncaughtExceptionHandler
        );

        command.submitted();

        try {
            executor.execute(command);
        } catch (RejectedExecutionException ex) {
            /*
             * 部分自定义拒绝策略会先调用 command.reject 再抛异常；
             * TaskExecutionRuntime 的 CAS 会保证拒绝状态只记录一次。
             */
            command.reject(ex);

            if (resultMode == TaskResultMode.FIRE_AND_FORGET) {
                AsyncError error = errorClassifier.classify(
                        task,
                        ex,
                        AsyncErrorStage.SUBMIT
                );
                throw new ConcurrencyRejectedException(
                        task.getExecutorName(),
                        task.getTaskName(),
                        error,
                        ex
                );
            }
        }

        if (resultMode == TaskResultMode.FIRE_AND_FORGET) {
            return baseFuture;
        }

        return resultPipeline.apply(context, command);
    }
}
