package com.xjtu.iron.concurrency.core.listener;

import com.xjtu.iron.concurrency.api.event.TaskExecutionEvent;
import com.xjtu.iron.concurrency.api.listener.TaskExecutionListener;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 组合任务执行监听器。
 *
 * <p>
 * 将多个 {@link TaskExecutionListener} 聚合成一个统一监听器，
 * 使任务主链路只依赖单个监听器对象。
 * </p>
 *
 * <p>
 * 每个真实监听器都会收到独立的事件副本；任意监听器抛出异常时，
 * 不会影响其他监听器，也不会影响任务执行主链路。
 * </p>
 */
public final class CompositeTaskExecutionListener implements TaskExecutionListener {

    /**
     * 按 Spring Order 或装配顺序排列的真实监听器列表。
     */
    private final List<TaskExecutionListener> listeners;

    /**
     * 创建组合监听器。
     *
     * @param listeners 需要组合的真实监听器列表
     */
    public CompositeTaskExecutionListener(List<TaskExecutionListener> listeners) {
        this.listeners = listeners == null
                ? Collections.emptyList()
                : listeners.stream()
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public void onSubmitted(TaskExecutionEvent event) {
        invoke(event, TaskExecutionListener::onSubmitted);
    }

    @Override
    public void onStarted(TaskExecutionEvent event) {
        invoke(event, TaskExecutionListener::onStarted);
    }

    @Override
    public void onSuccess(TaskExecutionEvent event) {
        invoke(event, TaskExecutionListener::onSuccess);
    }

    @Override
    public void onFailure(TaskExecutionEvent event) {
        invoke(event, TaskExecutionListener::onFailure);
    }

    @Override
    public void onRejected(TaskExecutionEvent event) {
        invoke(event, TaskExecutionListener::onRejected);
    }

    @Override
    public void onTimeout(TaskExecutionEvent event) {
        invoke(event, TaskExecutionListener::onTimeout);
    }

    @Override
    public void onCancelled(TaskExecutionEvent event) {
        invoke(event, TaskExecutionListener::onCancelled);
    }

    @Override
    public void onFallback(TaskExecutionEvent event) {
        invoke(event, TaskExecutionListener::onFallback);
    }

    @Override
    public void onFallbackSuccess(TaskExecutionEvent event) {
        invoke(event, TaskExecutionListener::onFallbackSuccess);
    }

    @Override
    public void onFallbackFailure(TaskExecutionEvent event) {
        invoke(event, TaskExecutionListener::onFallbackFailure);
    }

    @Override
    public void onCompleted(TaskExecutionEvent event) {
        invoke(event, TaskExecutionListener::onCompleted);
    }

    /**
     * 安全调用全部真实监听器。
     *
     * @param event 原始事件
     * @param invoker 具体生命周期方法调用器
     */
    private void invoke(TaskExecutionEvent event, ListenerInvoker invoker) {
        Objects.requireNonNull(event, "event must not be null");

        for (TaskExecutionListener listener : listeners) {
            try {
                invoker.invoke(listener, event.copy());
            } catch (Throwable ignored) {
                /*
                 * 监听器属于旁路扩展能力，不能覆盖原始任务结果。
                 * 后续接入组件内部日志后，可在此记录监听器类型和异常摘要。
                 */
            }
        }
    }

    /**
     * 单个监听器生命周期方法调用器。
     */
    @FunctionalInterface
    private interface ListenerInvoker {

        /**
         * 调用指定监听器。
         *
         * @param listener 真实监听器
         * @param event 独立事件副本
         */
        void invoke(TaskExecutionListener listener, TaskExecutionEvent event);
    }
}
