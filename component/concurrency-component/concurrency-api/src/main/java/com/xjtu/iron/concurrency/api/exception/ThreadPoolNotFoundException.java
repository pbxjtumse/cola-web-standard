package com.xjtu.iron.concurrency.api.exception;

/**
 * 线程池不存在异常。
 */
public class ThreadPoolNotFoundException extends ConcurrencyException {

    public ThreadPoolNotFoundException(String executorName) {
        super("Thread pool not found, executorName=" + executorName);
    }
}