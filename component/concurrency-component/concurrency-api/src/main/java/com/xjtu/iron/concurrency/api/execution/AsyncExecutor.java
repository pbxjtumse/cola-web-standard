package com.xjtu.iron.concurrency.api.execution;


import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 *
 * 异步执行器。
 *
 * <p>业务系统提交异步任务的统一入口。</p>
 */
public interface AsyncExecutor {

    /**
     * 提交一个有返回值的异步任务。
     *
     * @param executorName 线程池名称
     * @param taskName 任务名称，用于日志、指标、排查问题
     * @param supplier 任务逻辑
     * @return CompletableFuture
     */
    <T> CompletableFuture<T> supply(
            String executorName,
            String taskName,
            Supplier<T> supplier
    );

    /**
     * 提交一个无返回值的异步任务。
     *
     * @param executorName 线程池名称
     * @param taskName 任务名称
     * @param runnable 任务逻辑
     * @return CompletableFuture
     */
    CompletableFuture<Void> run(
            String executorName,
            String taskName,
            Runnable runnable
    );

    /**
     * 提交完整任务模型。
     *
     * <p>适合需要 timeout、fallback、上下文控制的复杂任务。</p>
     *
     * @param task 异步任务模型
     * @return CompletableFuture
     */
    <T> CompletableFuture<T> submit(AsyncTask<T> task);
}
