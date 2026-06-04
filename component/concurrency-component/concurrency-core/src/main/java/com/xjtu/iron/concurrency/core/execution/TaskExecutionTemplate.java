package com.xjtu.iron.concurrency.core.execution;

import com.xjtu.iron.concurrency.api.execution.AsyncTask;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * 任务投递模板。
 *
 * <p>这个接口是并行组件的一期核心：负责把任务安全地提交到 ThreadPoolExecutor。</p>
 * <p>AsyncTemplate 负责 CompletableFuture 编排；本模板负责任务提交链路。</p>
 */
public interface TaskExecutionTemplate {

    /**
     * 只提交任务，不关心结果。
     *
     * @param executorName 线程池名称
     * @param taskName 任务名称
     * @param runnable 任务逻辑
     */
    void execute(String executorName, String taskName, Runnable runnable);

    /**
     * 尝试提交任务。
     *
     * @param executorName 线程池名称
     * @param taskName 任务名称
     * @param runnable 任务逻辑
     * @return true 表示提交成功，false 表示被线程池拒绝
     */
    boolean tryExecute(String executorName, String taskName, Runnable runnable);

    /**
     * 提交无业务返回值任务，但返回 CompletableFuture 表示完成状态。
     *
     * @param executorName 线程池名称
     * @param taskName 任务名称
     * @param runnable 任务逻辑
     * @return 完成状态 Future
     */
    CompletableFuture<Void> run(String executorName, String taskName, Runnable runnable);

    /**
     * 提交有返回值任务。
     *
     * @param executorName 线程池名称
     * @param taskName 任务名称
     * @param supplier 任务逻辑
     * @param <T> 返回值类型
     * @return 结果 Future
     */
    <T> CompletableFuture<T> supply(String executorName, String taskName, Supplier<T> supplier);

    /**
     * 提交完整异步任务模型。
     *
     * @param task 异步任务模型
     * @param <T> 返回值类型
     * @return 结果 Future
     */
    <T> CompletableFuture<T> submit(AsyncTask<T> task);
}
