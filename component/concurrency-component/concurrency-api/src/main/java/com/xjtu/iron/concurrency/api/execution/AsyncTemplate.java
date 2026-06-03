package com.xjtu.iron.concurrency.api.execution;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * CompletableFuture 编排模板。
 * | 方法              | 是否等待全部任务 | 是否允许部分成功 | 任一失败会怎样      | 适合场景          |
 * | --------------- | -------: | -------: | ------------ | ------------- |
 * | `allOf`         |        是 |        否 | 整体失败         | 所有任务都是强依赖     |
 * | `anyOf`         |        否 |      不关心 | 第一个完成失败则可能失败 | 多路竞争，谁先回来用谁   |
 * | `allOfOutcome`  |        是 |        是 | 记录失败，不抛整体异常  | 页面聚合、非强依赖数据   |
 * | `allOfFailFast` |        否 |        否 | 立即整体失败       | 强依赖校验，失败就没必要等 |
 *
 * <p>用于封装 CompletableFuture 常见组合、超时、fallback、错误聚合等能力。</p>
 *
 * 如下还没有支持
 * 任务 DAG
 * 强制中断正在执行任务
 * 复杂重试编排
 * anySuccess
 *
 */
public interface AsyncTemplate {

    /**
     * 等待所有 Future 成功完成。
     * 所有任务都成功，返回结果列表。
     * 只要有一个任务失败，整体 Future 失败。
     * 如果任意一个 Future 异常，则返回的 Future 异常。
     * <p> 例如用户信息、订单信息、账户信息都必须查到，缺一个整体就失败 </p>
     * <p>特点</p>
     * <pre>
     *     优点：简单，适合强依赖场景。
     *     缺点：不能拿部分成功结果。
     * </pre>
     */
    <T> CompletableFuture<List<T>> allOf(Collection<CompletableFuture<T>> futures);

    /**
     * 等待所有 Future 完成，并返回每个任务的执行结果。
     *
     * <p>这个方法不会因为单个任务失败而让整体 Future 失败。</p>
     *
     * <p>适合部分成功场景。</p>
     */
    <T> CompletableFuture<AsyncBatchResult<T>> allOfOutcome(
            Collection<NamedFuture<T>> futures
    );

    /**
     * fail-fast 聚合。
     *
     * <p>任意一个 Future 失败，整体立即失败。 强依赖任务，只要一个失败，继续等其他任务没有意义。 </p>
     *
     * <p>注意：CompletableFuture 层面的 cancel 不一定能真正中断底层运行中的任务。</p>
     */
    <T> CompletableFuture<List<T>> allOfFailFast(
            Collection<CompletableFuture<T>> futures
    );

    /**
     * 谁先完成，就返回谁的结果。
     * 第一个完成的任务如果失败，整体也可能失败。注意 anyOf 是“第一个完成”，不是“第一个成功”。
     * 也就是说，第一个完成的是异常，它也会结束。
     */
    <T> CompletableFuture<T> anyOf(Collection<CompletableFuture<T>> futures);

    /**
     * 给 Future 增加结果层超时。
     *
     * <p>注意：它不保证强制中断底层正在执行的任务。</p>
     */
    <T> CompletableFuture<T> withTimeout(
            CompletableFuture<T> future,
            Duration timeout
    );

    /**
     * 给 Future 增加 fallback。
     */
    <T> CompletableFuture<T> withFallback(
            CompletableFuture<T> future,
            Function<Throwable, T> fallback
    );
}