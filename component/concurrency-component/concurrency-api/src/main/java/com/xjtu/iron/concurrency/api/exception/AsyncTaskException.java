package com.xjtu.iron.concurrency.api.exception;

/**
 * 异步任务执行异常。
 */
public class AsyncTaskException extends ConcurrencyException {

    public AsyncTaskException(String executorName, String taskName, Throwable cause) {
        super("Async task failed, executorName=" + executorName + ", taskName=" + taskName, cause);
    }
}