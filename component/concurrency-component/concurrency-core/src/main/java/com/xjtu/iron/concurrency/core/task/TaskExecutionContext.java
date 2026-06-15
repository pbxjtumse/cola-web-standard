package com.xjtu.iron.concurrency.core.task;



import com.xjtu.iron.concurrency.api.execution.task.AsyncTask;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * 任务执行上下文。
 *
 * <p>
 * 将任务定义、最终可执行逻辑、结果 Future 和运行时状态组合在一起。
 * </p>
 *
 * @param <T> 任务返回值类型
 */
public final class TaskExecutionContext<T> {

    /**
     * 用户提交的任务定义。
     */
    private final AsyncTask<T> task;

    /**
     * 真正在线程池工作线程中执行的逻辑。
     *
     * <p>
     * 该逻辑可能已经经过 MDC、TTL 等上下文装饰。
     * </p>
     */
    private final Supplier<T> executable;

    /**
     * 原始任务结果 Future。
     *
     * <p>
     * FIRE_AND_FORGET 模式下允许为空。
     * </p>
     */
    private final CompletableFuture<T> future;

    /**
     * 任务执行运行时状态。
     */
    private final TaskExecutionRuntime runtime;

    public TaskExecutionContext(
            AsyncTask<T> task,
            Supplier<T> executable,
            CompletableFuture<T> future,
            TaskExecutionRuntime runtime
    ) {
        this.task = Objects.requireNonNull(task, "task must not be null");
        this.executable = Objects.requireNonNull(
                executable,
                "executable must not be null"
        );
        this.future = future;
        this.runtime = Objects.requireNonNull(
                runtime,
                "runtime must not be null"
        );
    }

    public AsyncTask<T> getTask() {
        return task;
    }

    public Supplier<T> getExecutable() {
        return executable;
    }

    public CompletableFuture<T> getFuture() {
        return future;
    }

    public TaskExecutionRuntime getRuntime() {
        return runtime;
    }
}
