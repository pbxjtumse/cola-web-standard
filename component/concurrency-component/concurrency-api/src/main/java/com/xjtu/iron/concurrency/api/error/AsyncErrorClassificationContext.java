package com.xjtu.iron.concurrency.api.error;


import com.xjtu.iron.concurrency.api.enums.error.AsyncErrorStage;
import com.xjtu.iron.concurrency.api.execution.task.AsyncTask;

/**
 * 异步错误分类上下文。
 */
public final class AsyncErrorClassificationContext {

    /**
     * 当前异步任务。
     */
    private final AsyncTask<?> task;

    /**
     * 原始异常。
     */
    private final Throwable throwable;

    /**
     * 解包后的根因异常。
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
        this.task = task;
        this.throwable = throwable;
        this.rootCause = rootCause;
        this.stage = stage;
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
