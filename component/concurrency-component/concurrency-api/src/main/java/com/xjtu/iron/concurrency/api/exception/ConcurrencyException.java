package com.xjtu.iron.concurrency.api.exception;

/**
 * 并发组件基础异常。
 */
public class ConcurrencyException extends RuntimeException {

    public ConcurrencyException(String message) {
        super(message);
    }

    public ConcurrencyException(String message, Throwable cause) {
        super(message, cause);
    }
}
