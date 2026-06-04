package com.xjtu.iron.concurrency.core.execution;

import com.xjtu.iron.concurrency.api.enums.QueueType;
import com.xjtu.iron.concurrency.api.exception.ThreadPoolCreateException;
import com.xjtu.iron.concurrency.api.execution.ThreadPoolSpec;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 默认线程池工厂。
 *
 * <p>并行组件最底层明确使用 JDK ThreadPoolExecutor。</p>
 */
public class DefaultThreadPoolFactory implements ThreadPoolFactory {

    private final RejectedExecutionHandlerFactory rejectedExecutionHandlerFactory;

    public DefaultThreadPoolFactory() {
        this(new DefaultRejectedExecutionHandlerFactory());
    }

    public DefaultThreadPoolFactory(RejectedExecutionHandlerFactory rejectedExecutionHandlerFactory) {
        this.rejectedExecutionHandlerFactory = rejectedExecutionHandlerFactory;
    }

    @Override
    public ThreadPoolExecutor create(ThreadPoolSpec spec) {
        try {
            spec.validate();

            BlockingQueue<Runnable> queue = createQueue(spec);
            ThreadFactory threadFactory = createThreadFactory(spec);
            RejectedExecutionHandler rejectedExecutionHandler = rejectedExecutionHandlerFactory.create(spec);

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

    private ThreadFactory createThreadFactory(ThreadPoolSpec spec) {
        return new NamedThreadFactory(spec.getThreadNamePrefix());
    }
}
