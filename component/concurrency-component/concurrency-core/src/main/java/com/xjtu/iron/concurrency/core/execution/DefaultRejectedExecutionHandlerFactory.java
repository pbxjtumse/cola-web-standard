package com.xjtu.iron.concurrency.core.execution;

import com.xjtu.iron.concurrency.api.enums.RejectionPolicy;
import com.xjtu.iron.concurrency.api.execution.pool.ThreadPoolSpec;
import com.xjtu.iron.concurrency.core.spi.RejectedExecutionHandlerFactory;
import com.xjtu.iron.concurrency.core.task.RejectedTaskAware;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 默认拒绝策略工厂。
 */
public class DefaultRejectedExecutionHandlerFactory implements RejectedExecutionHandlerFactory {

    @Override
    public RejectedExecutionHandler create(ThreadPoolSpec spec) {
        RejectionPolicy policy = spec.getRejectionPolicy();

        if (policy == RejectionPolicy.CALLER_RUNS) {
            return callerRunsPolicy();
        }
        if (policy == RejectionPolicy.DISCARD) {
            return discardPolicy();
        }
        if (policy == RejectionPolicy.DISCARD_OLDEST) {
            return discardOldestPolicy();
        }
        if (policy == RejectionPolicy.BLOCKING_WAIT) {
            return new BlockingWaitRejectedExecutionHandler(spec.getRejectionWaitTime());
        }
        return new ThreadPoolExecutor.AbortPolicy();
    }

    private RejectedExecutionHandler callerRunsPolicy() {
        return (runnable, executor) -> {
            if (executor.isShutdown()) {
                throw new RejectedExecutionException("Executor already shutdown");
            }
            runnable.run();
        };
    }

    private RejectedExecutionHandler discardPolicy() {
        return (runnable, executor) -> {
            RejectedExecutionException ex = new RejectedExecutionException("Task discarded by DISCARD policy");
            if (runnable instanceof RejectedTaskAware rejectedTaskAware) {
                rejectedTaskAware.reject(ex);
            }
            throw ex;
        };
    }

    private RejectedExecutionHandler discardOldestPolicy() {
        return (runnable, executor) -> {
            if (executor.isShutdown()) {
                RejectedExecutionException ex = new RejectedExecutionException("Executor already shutdown");
                if (runnable instanceof RejectedTaskAware rejectedTaskAware) {
                    rejectedTaskAware.reject(ex);
                }
                throw ex;
            }

            Runnable oldest = executor.getQueue().poll();
            if (oldest instanceof RejectedTaskAware rejectedTaskAware) {
                rejectedTaskAware.reject(new RejectedExecutionException("Task discarded by DISCARD_OLDEST policy"));
            }
            executor.execute(runnable);
        };
    }
}
