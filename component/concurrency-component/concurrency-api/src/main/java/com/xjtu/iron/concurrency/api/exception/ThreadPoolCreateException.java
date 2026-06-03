package com.xjtu.iron.concurrency.api.exception;

/**
 * 线程池创建异常。
 */
public class ThreadPoolCreateException extends ConcurrencyException {

    public ThreadPoolCreateException(String poolName, Throwable cause) {
        super("Create thread pool failed, poolName=" + poolName, cause);
    }
}