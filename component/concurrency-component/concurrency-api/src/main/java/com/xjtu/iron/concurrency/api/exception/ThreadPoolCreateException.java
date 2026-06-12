package com.xjtu.iron.concurrency.api.exception;

import com.xjtu.iron.concurrency.api.error.AsyncError;

/**
 * 线程池创建异常。
 */
public class ThreadPoolCreateException extends ConcurrencyException {

    public ThreadPoolCreateException(String poolName, Throwable cause) {
        super("Create thread pool failed, poolName=" + poolName, AsyncError.unknown(cause), cause);
    }
}
