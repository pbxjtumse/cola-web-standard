package com.xjtu.iron.concurrency.core.support;

import com.xjtu.iron.concurrency.api.enums.task.AsyncTaskStatus;
import com.xjtu.iron.concurrency.api.event.TaskExecutionEvent;
import com.xjtu.iron.concurrency.api.listener.TaskExecutionListener;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 测试用监听器：记录生命周期事件顺序。
 */
public final class RecordingTaskExecutionListener implements TaskExecutionListener {

    private final List<String> events = new CopyOnWriteArrayList<>();

    @Override
    public void onSubmitted(TaskExecutionEvent event) {
        record(event);
    }

    @Override
    public void onStarted(TaskExecutionEvent event) {
        record(event);
    }

    @Override
    public void onSuccess(TaskExecutionEvent event) {
        record(event);
    }

    @Override
    public void onFailure(TaskExecutionEvent event) {
        record(event);
    }

    @Override
    public void onRejected(TaskExecutionEvent event) {
        record(event);
    }

    @Override
    public void onTimeout(TaskExecutionEvent event) {
        record(event);
    }

    @Override
    public void onCancelled(TaskExecutionEvent event) {
        record(event);
    }

    @Override
    public void onFallback(TaskExecutionEvent event) {
        record(event);
    }

    @Override
    public void onFallbackSuccess(TaskExecutionEvent event) {
        record(event);
    }

    @Override
    public void onFallbackFailure(TaskExecutionEvent event) {
        record(event);
    }

    @Override
    public void onCompleted(TaskExecutionEvent event) {
        events.add("COMPLETED:" + event.getStatus().name());
    }

    public List<String> events() {
        return List.copyOf(events);
    }

    public long count(AsyncTaskStatus status) {
        return events.stream().filter(status.name()::equals).count();
    }

    public long completedCount(AsyncTaskStatus status) {
        return events.stream().filter(("COMPLETED:" + status.name())::equals).count();
    }

    private void record(TaskExecutionEvent event) {
        events.add(event.getStatus().name());
    }
}
