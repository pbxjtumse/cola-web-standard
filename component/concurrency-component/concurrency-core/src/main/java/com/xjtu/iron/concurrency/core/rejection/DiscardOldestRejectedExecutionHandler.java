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
        Runnable oldest = queue.poll();

        if (oldest != null) {
            RejectedTaskSupport.reject(oldest, "Task discarded by DISCARD_OLDEST policy");
        }

        /*
         * 直接向队列 offer，避免再次调用 executor.execute 导致同一拒绝策略递归。
         */
        boolean accepted = queue.offer(runnable);

        /*
         * 在 poll 与 offer 之间可能有其他生产者抢占队列空位。
         */
        if (!accepted) {
            throw RejectedTaskSupport.reject(runnable, "Current task rejected after discarding oldest task");
        }

        /*
         * executor 可能在 offer 后并发关闭。若能够从队列移除当前任务，则明确标记拒绝；
         * 若任务已被工作线程取走，则由工作线程正常执行。
         */
        if (executor.isShutdown() && queue.remove(runnable)) {
            throw RejectedTaskSupport.reject(runnable, "Executor shutdown while enqueuing replacement task");
        }
    }
}
