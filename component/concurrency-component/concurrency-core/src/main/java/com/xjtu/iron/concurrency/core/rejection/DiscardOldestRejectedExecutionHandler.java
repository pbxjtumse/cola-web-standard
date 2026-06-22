package com.xjtu.iron.concurrency.core.rejection;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 拒绝感知版 DiscardOldest 策略。
 *
 * <p>
 * 丢弃队列中等待时间最长的任务，并尝试把当前任务放入队列。被丢弃的旧任务会收到
 * 明确的拒绝通知，避免其 CompletableFuture 永久不完成。
 * </p>
 */
public final class DiscardOldestRejectedExecutionHandler implements RejectedExecutionHandler {

    @Override
    public void rejectedExecution(Runnable runnable, ThreadPoolExecutor executor) {
        if (executor.isShutdown()) {
            throw RejectedTaskSupport.reject(runnable, "Executor already shutdown");
        }

        BlockingQueue<Runnable> queue = executor.getQueue();

        /*
         * 移除队列中等待时间最长的任务。
         */
        Runnable oldest = queue.poll();

        if (oldest != null) {
            /*
             * 被移除的旧任务不会再执行，必须明确通知它进入拒绝状态，
             * 避免旧任务对应的 CompletableFuture 永久未完成。
             *
             * 这里不向当前提交线程抛出旧任务异常，
             * 因为当前 runnable 仍然有机会成功进入队列。
             */
            RejectedTaskSupport.reject(oldest, "Task discarded by DISCARD_OLDEST policy");
        }

        /*
         * 直接向队列 offer，避免再次调用 executor.execute 导致同一拒绝策略递归。
         */
        boolean offered = queue.offer(runnable);

        /*
         * 在 poll 与 offer 之间可能有其他生产者抢占队列空位。
         */
        if (!offered) {
            /*
             * 旧任务被移除后的空位又被其他并发提交者抢走，
             * 当前任务也必须明确拒绝。
             */
            throw RejectedTaskSupport.reject(runnable, "Current task rejected after discarding oldest task");
        }
        /*
         * offer 成功不等于一定合法。
         * 等待期间线程池可能已经关闭。
         */
        RejectedTaskSupport.rejectIfShutdownAfterEnqueue(
                runnable,
                executor,
                "Executor shutdown while enqueuing replacement task"
        );
    }
}

