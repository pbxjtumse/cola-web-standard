package com.xjtu.iron.concurrency.core.task;

/**
 * 可感知拒绝的任务。
 *
 * <p>用于修复 DISCARD / DISCARD_OLDEST 这类策略导致 CompletableFuture 永远不完成的问题。</p>
 */
public interface RejectedTaskAware {

    /** 当任务被拒绝或被丢弃时调用。 */
    void reject(Throwable throwable);
}
