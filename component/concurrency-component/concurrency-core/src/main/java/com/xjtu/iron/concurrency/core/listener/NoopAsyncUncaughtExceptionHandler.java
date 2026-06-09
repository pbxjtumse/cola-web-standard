package com.xjtu.iron.concurrency.core.listener;

import com.xjtu.iron.concurrency.api.event.TaskExecutionEvent;
import com.xjtu.iron.concurrency.api.listener.AsyncUncaughtExceptionHandler;

public class NoopAsyncUncaughtExceptionHandler implements AsyncUncaughtExceptionHandler {
    @Override
    public void handleException(TaskExecutionEvent event, Throwable throwable) {

    }
}
