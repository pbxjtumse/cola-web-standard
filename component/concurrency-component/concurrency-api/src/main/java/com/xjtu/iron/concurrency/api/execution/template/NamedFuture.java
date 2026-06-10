package com.xjtu.iron.concurrency.api.execution.template;


import java.util.concurrent.CompletableFuture;

/**
 * 带名称的 Future。
 *
 * <p>用于批量异步任务编排时定位具体任务。</p>
 */
public class NamedFuture<T> {

    /**
     * 任务名称。
     */
    private final String taskName;

    /**
     * 任务 Future。
     */
    private final CompletableFuture<T> future;

    private NamedFuture(String taskName, CompletableFuture<T> future) {
        this.taskName = taskName;
        this.future = future;
    }

    public static <T> NamedFuture<T> of(String taskName, CompletableFuture<T> future) {
        return new NamedFuture<>(taskName, future);
    }

    public String getTaskName() {
        return taskName;
    }

    public CompletableFuture<T> getFuture() {
        return future;
    }
}
