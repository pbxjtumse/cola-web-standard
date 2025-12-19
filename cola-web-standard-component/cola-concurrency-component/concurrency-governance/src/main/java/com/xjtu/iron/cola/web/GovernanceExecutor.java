package com.xjtu.iron.cola.web;

import com.xjtu.iron.cola.web.context.GovernorContext;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * GovernanceExecutor 不创建 Executor，只“消费” Executor
 * @author pbxjt
 * @date 2025/12/19
 */
public interface GovernanceExecutor {

    /**
     * 同步执行
     *
     * @param context
     * @param task
     * @param executor
     * @return {@link T }
     * @throws Exception
     */
    <T> T execute(GovernorContext context, Callable<T> task, Executor executor) throws Exception;

    /**
     * 异步执行（fire-and-forget）
     *
     * @param context
     * @param task
     * @param executor
     */
    void executeAsync(GovernorContext context, Runnable task, Executor executor);

    /**
     * CompletableFuture 风格
     *
     * @param context
     * @param supplier
     * @param executor
     * @return {@link CompletableFuture }<{@link T }>
     */
    <T> CompletableFuture<T> executeFuture(GovernorContext context, Supplier<T> supplier, Executor executor);
}
