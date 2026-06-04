package com.xjtu.iron.concurrency.core.execution;

/**
 * 可感知拒绝的任务。
 *
 * <p>用于解决 DISCARD / DISCARD_OLDEST 这类策略导致 CompletableFuture 永远不完成的问题。</p>
 */
public interface RejectedTaskAware {

    void reject(Throwable ex);
}
