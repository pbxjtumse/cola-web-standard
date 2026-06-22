package com.xjtu.iron.concurrency.core.task;

import com.xjtu.iron.concurrency.api.context.ContextAwareTaskDecorator;
import com.xjtu.iron.concurrency.api.enums.error.AsyncErrorStage;
import com.xjtu.iron.concurrency.api.error.AsyncError;
import com.xjtu.iron.concurrency.api.error.AsyncErrorClassifier;
import com.xjtu.iron.concurrency.api.exception.ConcurrencyRejectedException;
import com.xjtu.iron.concurrency.api.execution.task.AsyncTask;
import com.xjtu.iron.concurrency.api.execution.task.TaskCancellationManager;
import com.xjtu.iron.concurrency.api.execution.task.TaskHandle;
import com.xjtu.iron.concurrency.api.listener.AsyncUncaughtExceptionHandler;
import com.xjtu.iron.concurrency.api.task.TaskResultMode;
import com.xjtu.iron.concurrency.core.control.DefaultTaskHandle;
import com.xjtu.iron.concurrency.core.control.TaskControl;
import com.xjtu.iron.concurrency.core.control.TaskControlRegistry;
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

    /** fire-and-forget 异常处理器。 */
    private final AsyncUncaughtExceptionHandler uncaughtExceptionHandler;

    /** 当前节点运行任务控制注册表。 */
    private final TaskControlRegistry taskControlRegistry;

    /** 当前节点任务取消管理器。 */
    private final TaskCancellationManager cancellationManager;

    public DefaultTaskExecutionTemplate(
            ThreadPoolRegistry threadPoolRegistry,
            ContextAwareTaskDecorator taskDecorator,
            TaskLifecyclePublisher lifecyclePublisher,
            TaskResultPipeline resultPipeline,
            AsyncErrorClassifier errorClassifier,
            AsyncUncaughtExceptionHandler uncaughtExceptionHandler,
            TaskControlRegistry taskControlRegistry,
            TaskCancellationManager cancellationManager
    ) {
        this.threadPoolRegistry = Objects.requireNonNull(
                threadPoolRegistry,
                "threadPoolRegistry must not be null"
        );
        this.taskDecorator = Objects.requireNonNull(
                taskDecorator,
                "taskDecorator must not be null"
        );
        this.lifecyclePublisher = Objects.requireNonNull(
                lifecyclePublisher,
                "lifecyclePublisher must not be null"
        );
        this.resultPipeline = Objects.requireNonNull(
                resultPipeline,
                "resultPipeline must not be null"
        );
        this.errorClassifier = Objects.requireNonNull(
                errorClassifier,
                "errorClassifier must not be null"
        );
        this.uncaughtExceptionHandler = Objects.requireNonNull(
                uncaughtExceptionHandler,
                "uncaughtExceptionHandler must not be null"
        );
        this.taskControlRegistry = Objects.requireNonNull(
                taskControlRegistry,
                "taskControlRegistry must not be null"
        );
        this.cancellationManager = Objects.requireNonNull(
                cancellationManager,
                "cancellationManager must not be null"
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
        Objects.requireNonNull(runnable, "runnable must not be null");
        AsyncTask<Void> task = AsyncTask.of(executorName, taskName, () -> {
            runnable.run();
            return null;
        });

        try {
            Submission<Void> submission = submitInternal(task, TaskResultMode.FIRE_AND_FORGET);
            return !submission.command().isRejected();
        } catch (ConcurrencyRejectedException rejected) {
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
        return submitHandle(task).getFuture();
    }

    @Override
    public <T> TaskHandle<T> submitHandle(AsyncTask<T> task) {
        return submitInternal(task, TaskResultMode.RESULT_AWARE).handle();
    }

    /**
     * 执行统一任务提交流程。
     */
    private <T> Submission<T> submitInternal(AsyncTask<T> task, TaskResultMode resultMode) {
        Objects.requireNonNull(task, "task must not be null");
        task.validate();
        TaskDefinition<T> definition = TaskDefinition.from(task);
        ThreadPoolExecutor executor = threadPoolRegistry.getExecutor(task.getExecutorName());
        CompletableFuture<T> baseFuture = new CompletableFuture<>();
        Supplier<T> executable = task.isContextPropagation()
                ? taskDecorator.decorate(task.getOperation())
                : task.getOperation();
        TaskExecutionRuntime runtime = new TaskExecutionRuntime(resultMode);
        TaskExecutionContext<T> context = new TaskExecutionContext<>(task, executable, baseFuture, runtime);
        TaskCommand<T> command = new TaskCommand<>(context, lifecyclePublisher, errorClassifier, uncaughtExceptionHandler);

        CompletableFuture<T> finalFuture = resultMode == TaskResultMode.RESULT_AWARE
                ? resultPipeline.apply(context, command)
                : baseFuture;
        TaskControl<T> control = new TaskControl<>(executor, command, finalFuture);
        TaskHandle<T> handle = new DefaultTaskHandle<>(task.getTaskId(), finalFuture, cancellationManager);
        taskControlRegistry.register(task.getTaskId(), control);
        finalFuture.whenComplete((value, throwable) -> taskControlRegistry.remove(task.getTaskId(), control));
        command.submitted();
        try {
            executor.execute(command);
        } catch (RejectedExecutionException rejected) {
            /*
             * ABORT、BLOCKING_WAIT 和线程池关闭的 CALLER_RUNS 会同步抛异常。
             * 自定义处理器通常已经先通知 command.reject；CAS 保证重复调用不会重复发布。
             */
            command.reject(rejected);

            if (resultMode == TaskResultMode.FIRE_AND_FORGET) {
                AsyncError error = errorClassifier.classify(task, rejected, AsyncErrorStage.SUBMIT);
                throw new ConcurrencyRejectedException(task.getExecutorName(), task.getTaskName(), error, rejected);
            }
        }

        return new Submission<>(handle, command);
    }

    /**
     * 内部提交结果，同时保留句柄和命令，供 tryExecute 判断同步拒绝。
     */
    private record Submission<T>(TaskHandle<T> handle, TaskCommand<T> command) {
    }
}
