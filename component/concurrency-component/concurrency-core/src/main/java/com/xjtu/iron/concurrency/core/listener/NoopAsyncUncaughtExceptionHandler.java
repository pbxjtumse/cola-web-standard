package com.xjtu.iron.concurrency.core.listener;

import com.xjtu.iron.concurrency.api.listener.AsyncUncaughtExceptionHandler;
import com.xjtu.iron.concurrency.api.listener.TaskExecutionEvent;

/**
 * 默认 fire-and-forget 异常处理器。
 */
public class NoopAsyncUncaughtExceptionHandler implements AsyncUncaughtExceptionHandler {

    @Override
    public void handleException(TaskExecutionEvent event, Throwable throwable) {
        // intentionally noop
    }
}
