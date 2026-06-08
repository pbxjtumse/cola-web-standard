package com.xjtu.iron.concurrency.core.listener;

import com.xjtu.iron.concurrency.api.event.TaskExecutionEvent;
import com.xjtu.iron.concurrency.api.listener.AsyncUncaughtExceptionHandler;

/**
 * 默认 fire-and-forget 异常处理器。
 *
 * <p>默认不做任何处理。生产环境建议业务侧提供自己的实现，至少输出日志或告警。</p>
 */
public class NoopAsyncUncaughtExceptionHandler implements AsyncUncaughtExceptionHandler {

    @Override
    public void handleException(TaskExecutionEvent event, Throwable throwable) {
        // intentionally noop
    }
}
