package com.xjtu.iron.concurrency.core.listener;

import com.xjtu.iron.concurrency.api.event.TaskExecutionEvent;
import com.xjtu.iron.concurrency.api.listener.TaskExecutionListener;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 组合任务执行监听器。组件内部组合实现
 *
 * <p>
 * 该类用于将多个 {@link TaskExecutionListener} 聚合为一个监听器，
 * 使任务执行主链路只依赖一个监听器对象。
 * </p>
 *
 * <p>
 * 任意一个监听器抛出异常时，不会影响其他监听器，也不会影响任务主执行链路。
 * </p>
 */
public class CompositeTaskExecutionListener implements TaskExecutionListener {

    private final List<TaskExecutionListener> listeners;

    public CompositeTaskExecutionListener(List<TaskExecutionListener> listeners) {
        this.listeners = listeners == null
                ? Collections.emptyList()
                : listeners.stream()
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public void onSubmitted(TaskExecutionEvent event) {
        invoke(listener -> listener.onSubmitted(event));
    }

    @Override
    public void onStarted(TaskExecutionEvent event) {
        invoke(listener -> listener.onStarted(event));
    }

    @Override
    public void onSuccess(TaskExecutionEvent event) {
        invoke(listener -> listener.onSuccess(event));
    }

    @Override
    public void onFailure(TaskExecutionEvent event) {
        invoke(listener -> listener.onFailure(event));
    }

    @Override
    public void onRejected(TaskExecutionEvent event) {
        invoke(listener -> listener.onRejected(event));
    }

    @Override
    public void onTimeout(TaskExecutionEvent event) {
        invoke(listener -> listener.onTimeout(event));
    }

    @Override
    public void onFallback(TaskExecutionEvent event) {
        invoke(listener -> listener.onFallback(event));
    }

    @Override
    public void onCompleted(TaskExecutionEvent event) {
        invoke(listener -> listener.onCompleted(event));
    }

    private void invoke(ListenerInvoker invoker) {
        for (TaskExecutionListener listener : listeners) {
            try {
                invoker.invoke(listener);
            } catch (Throwable ignored) {
                // 监听器不能影响任务执行主链路。
                // 后续可以接入内部 logger，但不要继续抛出。
            }
        }
    }

    @FunctionalInterface
    private interface ListenerInvoker {
        void invoke(TaskExecutionListener listener);
    }
}