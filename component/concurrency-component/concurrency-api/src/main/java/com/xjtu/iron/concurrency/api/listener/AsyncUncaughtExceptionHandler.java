package com.xjtu.iron.concurrency.api.listener;

/**
 * fire-and-forget 任务异常处理器。
 *
 * <p>{@code AsyncExecutor.execute(...)} 不返回 {@code CompletableFuture}，调用方无法直接感知异步异常。
 * 该接口用于统一兜底处理这类异常。</p>
 */
public interface AsyncUncaughtExceptionHandler {

    /**
     * 处理 fire-and-forget 任务中的未捕获异常。
     *
     * @param event 任务事件
     * @param throwable 异常对象
     */
    void handleException(TaskExecutionEvent event, Throwable throwable);
}
