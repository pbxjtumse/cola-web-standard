package com.xjtu.iron.concurrency.core.execution;

import com.xjtu.iron.concurrency.api.enums.QueueType;
import com.xjtu.iron.concurrency.api.enums.RejectionPolicy;
import com.xjtu.iron.concurrency.api.exception.ThreadPoolCreateException;
import com.xjtu.iron.concurrency.api.execution.ThreadPoolSpec;

import java.util.concurrent.*;

/**
 * 默认线程池工厂。
 */
public class DefaultThreadPoolFactory implements ThreadPoolFactory {

    @Override
    public ExecutorService create(ThreadPoolSpec spec) {
        try {
            spec.validate();

            BlockingQueue<Runnable> queue = createQueue(spec);
            ThreadFactory threadFactory = createThreadFactory(spec);
            RejectedExecutionHandler rejectedExecutionHandler = createRejectedExecutionHandler(spec);

            ThreadPoolExecutor executor = new ThreadPoolExecutor(
                    spec.getCorePoolSize(),
                    spec.getMaxPoolSize(),
                    spec.getKeepAliveTime().toMillis(),
                    TimeUnit.MILLISECONDS,
                    queue,
                    threadFactory,
                    rejectedExecutionHandler
            );

            executor.allowCoreThreadTimeOut(spec.isAllowCoreThreadTimeout());

            return executor;
        } catch (Exception ex) {
            throw new ThreadPoolCreateException(spec == null ? "unknown" : spec.getName(), ex);
        }
    }

    /**
     * 创建工作队列。
     */
    private BlockingQueue<Runnable> createQueue(ThreadPoolSpec spec) {
        QueueType queueType = spec.getQueueType();

        if (queueType == QueueType.BOUNDED_ARRAY_BLOCKING_QUEUE) {
            return new ArrayBlockingQueue<>(spec.getQueueCapacity());
        }

        if (queueType == QueueType.DIRECT_HANDOFF) {
            return new SynchronousQueue<>();
        }

        return new LinkedBlockingQueue<>(spec.getQueueCapacity());
    }

    /**
     * 创建线程工厂。
     */
    private ThreadFactory createThreadFactory(ThreadPoolSpec spec) {
        return new NamedThreadFactory(spec.getThreadNamePrefix());
    }

    /**
     * 创建拒绝策略。
     */
    private RejectedExecutionHandler createRejectedExecutionHandler(ThreadPoolSpec spec) {
        RejectionPolicy policy = spec.getRejectionPolicy();

        if (policy == RejectionPolicy.CALLER_RUNS) {
            return new ThreadPoolExecutor.CallerRunsPolicy();
        }

        if (policy == RejectionPolicy.DISCARD) {
            return new ThreadPoolExecutor.DiscardPolicy();
        }

        if (policy == RejectionPolicy.DISCARD_OLDEST) {
            return new ThreadPoolExecutor.DiscardOldestPolicy();
        }

        if (policy == RejectionPolicy.BLOCKING_WAIT) {
            return new BlockingWaitRejectedExecutionHandler(spec.getRejectionWaitTime());
        }

        return new ThreadPoolExecutor.AbortPolicy();
    }
}