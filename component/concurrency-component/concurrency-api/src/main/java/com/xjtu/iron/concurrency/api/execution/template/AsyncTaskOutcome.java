package com.xjtu.iron.concurrency.api.execution.template;


/**
 * 单个异步任务执行结果。
 *
 * @param <T> 任务返回值类型
 */
public class AsyncTaskOutcome<T> {

    /**
     * 任务名称。
     */
    private String taskName;

    /**
     * 是否成功。
     */
    private boolean success;

    /**
     * 任务返回值。
     */
    private T value;

    /**
     * 任务异常。
     */
    private Throwable error;

    public static <T> AsyncTaskOutcome<T> success(String taskName, T value) {
        AsyncTaskOutcome<T> outcome = new AsyncTaskOutcome<>();
        outcome.taskName = taskName;
        outcome.success = true;
        outcome.value = value;
        return outcome;
    }

    public static <T> AsyncTaskOutcome<T> failure(String taskName, Throwable error) {
        AsyncTaskOutcome<T> outcome = new AsyncTaskOutcome<>();
        outcome.taskName = taskName;
        outcome.success = false;
        outcome.error = error;
        return outcome;
    }

    public String getTaskName() {
        return taskName;
    }

    public boolean isSuccess() {
        return success;
    }

    public T getValue() {
        return value;
    }

    public Throwable getError() {
        return error;
    }
}
