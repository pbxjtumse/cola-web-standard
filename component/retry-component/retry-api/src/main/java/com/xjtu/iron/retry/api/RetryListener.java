package com.xjtu.iron.retry.api;

/** 监听重试生命周期事件。 */
@FunctionalInterface
public interface RetryListener {

    /**
     * 接收一个不可变重试事件。
     *
     * <p>监听器抛出的运行时异常会被默认执行器隔离，不能改变业务执行结果。</p>
     *
     * @param event 重试事件
     */
    void onEvent(RetryEvent event);
}
