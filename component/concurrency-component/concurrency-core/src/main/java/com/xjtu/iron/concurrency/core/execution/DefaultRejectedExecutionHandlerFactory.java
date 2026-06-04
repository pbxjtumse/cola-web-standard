package com.xjtu.iron.concurrency.core.execution;

import com.xjtu.iron.concurrency.api.enums.RejectionPolicy;
import com.xjtu.iron.concurrency.api.execution.ThreadPoolSpec;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 默认拒绝策略工厂。
 *
 * <p>这里对 DISCARD / DISCARD_OLDEST 做了 Future 感知增强，避免 CompletableFuture 永远不完成。</p>
 */
public class DefaultRejectedExecutionHandlerFactory implements RejectedExecutionHandlerFactory {

    @Override
    public RejectedExecutionHandler create(ThreadPoolSpec spec) {
        RejectionPolicy policy = spec.getRejectionPolicy();

        if (policy == RejectionPolicy.CALLER_RUNS) {
            return new FutureAwareCallerRunsPolicy();
        }

        if (policy == RejectionPolicy.DISCARD) {
            return new NotifyingDiscardPolicy();
        }

        if (policy == RejectionPolicy.DISCARD_OLDEST) {
            return new FutureAwareDiscardOldestPolicy();
        }

        if (policy == RejectionPolicy.BLOCKING_WAIT) {
            return new BlockingWaitRejectedExecutionHandler(spec.getRejectionWaitTime());
        }

        return new ThreadPoolExecutor.AbortPolicy();
    }

    /**
     * CallerRuns 增强版：线程池关闭时不静默丢弃，而是抛异常。
     */
    private static class FutureAwareCallerRunsPolicy implements RejectedExecutionHandler {
        @Override
        public void rejectedExecution(Runnable runnable, ThreadPoolExecutor executor) {
            if (executor.isShutdown()) {
                throw new RejectedExecutionException("Executor already shutdown");
            }
            runnable.run();
        }
    }

    /**
     * Discard 增强版：丢弃当前任务，但通知调用方，不静默。
     */
    private static class NotifyingDiscardPolicy implements RejectedExecutionHandler {
        @Override
        public void rejectedExecution(Runnable runnable, ThreadPoolExecutor executor) {
            throw new RejectedExecutionException("Task discarded by DISCARD policy");
        }
    }

    /**
     * DiscardOldest 增强版：丢弃队列最老任务时，如果它是组件任务，则让其 Future 异常完成。
     */
    private static class FutureAwareDiscardOldestPolicy implements RejectedExecutionHandler {
        @Override
        public void rejectedExecution(Runnable runnable, ThreadPoolExecutor executor) {
            if (executor.isShutdown()) {
                throw new RejectedExecutionException("Executor already shutdown");
            }

            Runnable oldest = executor.getQueue().poll();
            if (oldest instanceof RejectedTaskAware) {
                ((RejectedTaskAware) oldest).reject(new RejectedExecutionException("Task discarded by DISCARD_OLDEST policy"));
            }

            executor.execute(runnable);
        }
    }
}
