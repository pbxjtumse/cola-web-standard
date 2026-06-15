package com.xjtu.iron.concurrency.api.error;

import com.xjtu.iron.concurrency.api.enums.error.AsyncErrorStage;
import com.xjtu.iron.concurrency.api.execution.task.AsyncTask;

import java.util.Objects;

/**
 * 异步错误分类上下文。
 *
 * <p>
 * 一条分类规则需要同时知道任务元数据、原始异常、根因异常和异常发生阶段，
 * 因此使用该对象统一传递分类输入。
 * </p>
 */
public final class AsyncErrorClassificationContext {

    /**
     * 当前异步任务。
     */
    private final AsyncTask<?> task;

    /**
     * 尚未剥离 CompletionException、ExecutionException 等包装的原始异常。
     */
    private final Throwable throwable;

    /**
     * 已经剥离包装并遍历 cause 链后得到的根因异常。
     */
    private final Throwable rootCause;

    /**
     * 异常发生阶段。
     */
    private final AsyncErrorStage stage;

    public AsyncErrorClassificationContext(
            AsyncTask<?> task,
            Throwable throwable,
            Throwable rootCause,
            AsyncErrorStage stage
    ) {
        this.task = Objects.requireNonNull(task, "task must not be null");
        this.throwable = throwable;
        this.rootCause = rootCause;
        this.stage = stage == null ? AsyncErrorStage.NONE : stage;
    }

    public AsyncTask<?> getTask() {
        return task;
    }

    public Throwable getThrowable() {
        return throwable;
    }

    public Throwable getRootCause() {
        return rootCause;
    }

    public AsyncErrorStage getStage() {
        return stage;
    }
}
