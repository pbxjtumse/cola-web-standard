package com.xjtu.iron.concurrency.core.listener;


import com.xjtu.iron.concurrency.api.event.TaskExecutionEvent;
import com.xjtu.iron.concurrency.api.listener.TaskExecutionListener;

/**
 * 空任务执行监听器。
 *
 * <p>当用户没有配置任何监听器时使用。</p>
 */
public class NoopTaskExecutionListener implements TaskExecutionListener {

    @Override
    public void onSubmitted(TaskExecutionEvent event) {
    }

    @Override
    public void onStarted(TaskExecutionEvent event) {
    }

    @Override
    public void onSuccess(TaskExecutionEvent event) {
    }

    @Override
    public void onFailure(TaskExecutionEvent event) {
    }

    @Override
    public void onRejected(TaskExecutionEvent event) {
    }

    @Override
    public void onTimeout(TaskExecutionEvent event) {
    }

    @Override
    public void onFallback(TaskExecutionEvent event) {
    }

    @Override
    public void onCompleted(TaskExecutionEvent event) {
    }
}
