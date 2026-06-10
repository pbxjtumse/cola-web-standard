package com.xjtu.iron.concurrency.api.exception;

/**
 * 并发组件基础异常。
 */

import com.xjtu.iron.concurrency.api.error.AsyncError;

/**
 * 并行组件基础异常。
 *
 * <p>
 * 所有并行组件内部抛出的异常建议继承该类。
 * 该异常携带 AsyncError，方便上层识别错误分类、恢复建议和补偿信息。
 * </p>
 */
public class ConcurrencyException extends RuntimeException {

    /**
     * 异步错误详情。
     */
    private final AsyncError error;

    public ConcurrencyException(String message, AsyncError error) {
        super(message);
        this.error = error == null ? AsyncError.none() : error;
    }

    public ConcurrencyException(String message, AsyncError error, Throwable cause) {
        super(message, cause);
        this.error = error == null ? AsyncError.none() : error;
    }

    public AsyncError getError() {
        return error;
    }
}
