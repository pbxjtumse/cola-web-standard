package com.xjtu.iron.concurrency.api.execution.task;

import java.util.concurrent.CompletableFuture;

/**
 * 可控制的异步任务句柄。
 *
 * <p>
 * 业务方需要主动取消任务时，应优先使用 {@link #cancel(boolean)}，
 * 而不是直接调用 {@link CompletableFuture#cancel(boolean)}。
 * 通过句柄取消才能同步更新 CANCELLED 状态、监听器、指标和运行任务注册表。
 * </p>
 *
 * @param <T> 任务结果类型
 */
public interface TaskHandle<T> {

    /**
     * 获取任务唯一 ID。
     *
     * @return 任务唯一 ID
     */
    String getTaskId();

    /**
     * 获取最终返回给调用方的 Future。
     *
     * @return 应用 timeout、fallback 后的最终 Future
     */
    CompletableFuture<T> getFuture();

    /**
     * 主动取消任务。
     *
     * @param mayInterruptIfRunning 是否尽力中断正在执行的原始任务或 fallback 线程
     * @return 取消结果
     */
    TaskCancelResult cancel(boolean mayInterruptIfRunning);

    /**
     * 判断最终 Future 是否已经完成。
     *
     * @return 是否完成
     */
    default boolean isDone() {
        return getFuture().isDone();
    }
}
