package com.xjtu.iron.concurrency.api.exception;

/**
 * 任务被线程池拒绝异常。
 */
public class ConcurrencyRejectedException extends ConcurrencyException {

    public ConcurrencyRejectedException(String executorName, String taskName, Throwable cause) {
        super("Async task rejected, executorName=" + executorName + ", taskName=" + taskName, cause);
    }
}