package com.xjtu.iron.concurrency.api.listener;

import com.xjtu.iron.concurrency.api.event.TaskExecutionEvent;

/**
 * fire-and-forget 异步任务异常处理器。
 *
 * <p>{@code AsyncExecutor.execute(...)} 不返回 CompletableFuture，调用方无法通过返回值感知任务异常。</p>
 *
 * <p>因此 execute 任务执行失败时，组件会调用这个处理器。业务系统可以把异常写入日志、告警、审计或补偿队列。</p>
 */
public interface AsyncUncaughtExceptionHandler {

    /**
     * 处理未被调用方感知的异步异常。
     *
     * @param event 任务执行事件
     * @param throwable 异常对象
     */
    void handleException(TaskExecutionEvent event, Throwable throwable);
}
