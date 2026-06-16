package com.xjtu.iron.concurrency.core.rejection;

import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 阻塞等待拒绝策略。
 *
 * <p>
 * 当线程池工作线程和队列都已饱和时，提交线程最多等待指定时间获取队列空位。
 * 等待超时、线程池关闭或提交线程被中断时，任务会被明确标记为拒绝。
 * </p>
 */
public final class BlockingWaitRejectedExecutionHandler implements RejectedExecutionHandler {

    /**
     * 提交线程最多等待队列空位的时间。
     */
    private final Duration waitTime;

    /**
     * 创建阻塞等待策略。
     *
     * @param waitTime 最长等待时间；为空时按 0 处理
     */
    public BlockingWaitRejectedExecutionHandler(Duration waitTime) {
        this.waitTime = waitTime == null || waitTime.isNegative()
                ? Duration.ZERO
                : waitTime;
    }

    @Override
    public void rejectedExecution(Runnable runnable, ThreadPoolExecutor executor) {
        if (executor.isShutdown()) {
            throw RejectedTaskSupport.reject(runnable, "Executor already shutdown");
        }

        BlockingQueue<Runnable> queue = executor.getQueue();

        try {
            boolean accepted = queue.offer(
                    runnable,
                    waitTime.toMillis(),
                    TimeUnit.MILLISECONDS
            );

            if (!accepted) {
                throw RejectedTaskSupport.reject(
                        runnable,
                        "Task rejected after waiting "
                                + waitTime.toMillis()
                                + " ms for queue capacity"
                );
            }

            if (executor.isShutdown() && queue.remove(runnable)) {
                throw RejectedTaskSupport.reject(
                        runnable,
                        "Executor shutdown while waiting to enqueue task"
                );
            }
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw RejectedTaskSupport.reject(
                    runnable,
                    "Submitting thread interrupted while waiting to enqueue task",
                    interruptedException
            );
        }
    }
}
