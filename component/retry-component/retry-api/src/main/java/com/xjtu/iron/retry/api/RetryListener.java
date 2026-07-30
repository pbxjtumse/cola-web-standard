package com.xjtu.iron.retry.api;

/**
 * 重试生命周期监听器。
 */
@FunctionalInterface
public interface RetryListener {

    /**
     * 接收重试生命周期事件。
     *
     * <p>监听器不应该抛出异常。即使抛出异常，默认执行器也会隔离该异常。</p>
     */
    void onEvent(RetryEvent event);
}
