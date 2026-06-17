package com.xjtu.iron.concurrency.core.task;

import com.xjtu.iron.concurrency.api.enums.task.AsyncTaskStatus;
import com.xjtu.iron.concurrency.api.error.AsyncError;
import com.xjtu.iron.concurrency.api.event.TaskExecutionEvent;
import com.xjtu.iron.concurrency.api.execution.task.AsyncTask;
import com.xjtu.iron.concurrency.api.task.TaskMetadata;

import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * 单次任务执行上下文。
 *
 * <p>
 * {@link AsyncTask} 保存用户提交的原始任务定义；本类保存本次执行实例使用的固定元数据、
 * 装饰后逻辑、原始结果 Future 和动态运行时状态。
 * </p>
 *
 * <p>
 * TaskCommand 只以该上下文作为数据来源，避免任务定义、可执行逻辑和运行状态
 * 分散在多个字段中。
 * </p>
 *
 * @param <T> 任务返回值类型
 */
public final class TaskExecutionContext<T> {

    /**
     * 用户提交的任务定义。
     *
     * <p>用于读取 timeout、queueTimeout、fallback 等任务配置。</p>
     */
    private final AsyncTask<T> task;

    /**
     * 本次执行固定的只读任务元数据。
     *
     * <p>
     * 在上下文创建时从 AsyncTask 中提取。后续所有生命周期事件都复用同一份元数据，
     * 避免任务提交后被误修改造成前后事件不一致。
     * </p>
     */
    private final TaskMetadata metadata;

    /**
     * 真正在线程池中执行的逻辑。
     *
     * <p>该逻辑可能已经经过 MDC、TTL 或其他上下文传播装饰。</p>
     */
    private final Supplier<T> executable;

    /**
     * 原始任务结果 Future。
     *
     * <p>即使是 FIRE_AND_FORGET 模式也会创建，便于组件内部统一维护结果状态。</p>
     */
    private final CompletableFuture<T> baseFuture;

    /**
     * 本次执行的动态运行时状态。
     */
    private final TaskExecutionRuntime runtime;

    /**
     * 创建单次任务执行上下文。
     *
     * <p>调用前必须先执行 {@link AsyncTask#validate()}。</p>
     *
     * @param task 用户任务定义
     * @param executable 装饰后的最终执行逻辑
     * @param baseFuture 原始任务结果 Future
     * @param runtime 动态运行时状态
     */
    public TaskExecutionContext(
            AsyncTask<T> task,
            Supplier<T> executable,
            CompletableFuture<T> baseFuture,
            TaskExecutionRuntime runtime
    ) {
        this.task = Objects.requireNonNull(task, "task must not be null");
        this.metadata = Objects.requireNonNull(
                task.metadata(),
                "task metadata must not be null"
        );
        this.executable = Objects.requireNonNull(
                executable,
                "executable must not be null"
        );
        this.baseFuture = Objects.requireNonNull(
                baseFuture,
                "baseFuture must not be null"
        );
        this.runtime = Objects.requireNonNull(
                runtime,
                "runtime must not be null"
        );
    }

    /**
     * 根据当前运行时状态创建生命周期事件。
     *
     * @param status 本次事件状态
     * @param error 结构化错误；正常事件使用 AsyncError.none()
     * @param message 事件说明
     * @return 只读任务事件
     */
    public TaskExecutionEvent event(
            AsyncTaskStatus status,
            AsyncError error,
            String message
    ) {
        return new TaskExecutionEvent(
                metadata,
                status,
                runtime.getResultMode(),
                runtime.getExecutionMode(),
                runtime.timingSnapshot(),
                error,
                message,
                Instant.now()
        );
    }

    /**
     * 判断当前任务是否配置 fallback。
     *
     * @return 是否配置 fallback
     */
    public boolean hasFallback() {
        return task.getFallback() != null;
    }

    public AsyncTask<T> getTask() {
        return task;
    }

    public TaskMetadata getMetadata() {
        return metadata;
    }

    public Supplier<T> getExecutable() {
        return executable;
    }

    public CompletableFuture<T> getBaseFuture() {
        return baseFuture;
    }

    public TaskExecutionRuntime getRuntime() {
        return runtime;
    }
}
