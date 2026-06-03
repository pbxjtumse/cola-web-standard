package com.xjtu.iron.concurrency.core.execution;


import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 阻塞等待拒绝策略。
 *
 * <p>当线程池满时，尝试等待一段时间把任务放入队列。</p>
 */
public class BlockingWaitRejectedExecutionHandler implements RejectedExecutionHandler {

    private final Duration waitTime;

    public BlockingWaitRejectedExecutionHandler(Duration waitTime) {
        this.waitTime = waitTime == null ? Duration.ZERO : waitTime;
    }

    @Override
    public void rejectedExecution(Runnable runnable, ThreadPoolExecutor executor) {
        if (executor.isShutdown()) {
            throw new RejectedExecutionException("Executor already shutdown");
        }

        try {
            BlockingQueue<Runnable> queue = executor.getQueue();

            boolean offered = queue.offer(
                    runnable,
                    waitTime.toMillis(),
                    TimeUnit.MILLISECONDS
            );

            if (!offered) {
                throw new RejectedExecutionException(
                        "Task rejected after waiting " + waitTime.toMillis() + "ms"
                );
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RejectedExecutionException("Interrupted while waiting to enqueue task", ex);
        }
    }
}
