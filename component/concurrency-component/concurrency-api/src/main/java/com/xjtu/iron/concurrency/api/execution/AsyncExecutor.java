package com.xjtu.iron.concurrency.api.execution;

import com.xjtu.iron.concurrency.api.exception.ConcurrencyRejectedException;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * 异步执行器 负责提交单个任务
 *
 * <p>业务系统提交异步任务的统一入口。</p>
 */
public interface AsyncExecutor {

    /**
     * 只提交，不关心结果
     *
     * <p>不返回 Future，不参与 CompletableFuture 编排。</p>
     * <p>适合 fire-and-forget 场景，例如异步日志、异步通知、轻量后台任务。</p>
     *
     * <p>注意：任务执行异常无法由调用方通过 Future 感知，只能依赖日志、指标、告警。</p>
     *
     * @param executorName 线程池名称
     * @param taskName 任务名称
     * @param runnable 任务逻辑
     */
    void execute(String executorName, String taskName, Runnable runnable);

    /**
     * 尝试提交，失败返回 false
     *
     * <p>被线程池拒绝时返回 false，不抛异常。</p>
     */
    default boolean tryExecute(String executorName, String taskName, Runnable runnable) {
        try {
            execute(executorName, taskName, runnable);
            return true;
        } catch (ConcurrencyRejectedException ex) {
            return false;
        }
    }

    /**
     * 提交一个无返回值的异步任务。
     *
     * <p>返回 CompletableFuture，适合调用方需要感知完成、异常、超时或参与编排的场景。</p>
     *
     * @param executorName 线程池名称
     * @param taskName 任务名称
     * @param runnable 任务逻辑
     * @return CompletableFuture
     */
    CompletableFuture<Void> run(String executorName, String taskName, Runnable runnable);



    /**
     * 提交一个有返回值的异步任务。
     *
     * <p>返回 CompletableFuture，适合后续做 allOf / anyOf / timeout / fallback 等编排。</p>
     *
     * @param executorName 线程池名称
     * @param taskName 任务名称，用于日志、指标、排查问题
     * @param supplier 任务逻辑
     * @return CompletableFuture
     */
    <T> CompletableFuture<T> supply(String executorName, String taskName, Supplier<T> supplier);



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
