package com.xjtu.iron.concurrency.api.execution.executor;

import com.xjtu.iron.concurrency.api.exception.ConcurrencyRejectedException;
import com.xjtu.iron.concurrency.api.execution.task.AsyncTask;
import com.xjtu.iron.concurrency.api.execution.template.AsyncTemplate;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * 异步执行器。
 *
 * <p>业务系统提交异步任务的统一入口。它只负责提交单个任务；多个任务的组合编排交给 {@link AsyncTemplate}。</p>
 */
public interface AsyncExecutor {

    /**
     * 只提交任务，不关心结果。
     *
     * <p>适合异步日志、埋点、弱依赖通知、缓存清理等 fire-and-forget 场景。</p>
     */
    void execute(String executorName, String taskName, Runnable runnable);

    /**
     * 尝试提交任务。
     *
     * <p>只有线程池拒绝时返回 {@code false}；参数错误、线程池不存在等问题仍然会抛异常。</p>
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
     * 提交一个有返回值的异步任务。
     */
    <T> CompletableFuture<T> supply(String executorName, String taskName, Supplier<T> supplier);

    /**
     * 提交一个无业务返回值的异步任务，但可以通过 {@code CompletableFuture<Void>} 感知完成和异常。
     */
    CompletableFuture<Void> run(String executorName, String taskName, Runnable runnable);

    /**
     * 提交完整异步任务模型。
     *
     * <p>适合需要 timeout、fallback、queueTimeout、上下文控制、任务元数据的复杂任务。</p>
     */
    <T> CompletableFuture<T> submit(AsyncTask<T> task);
}
