package com.xjtu.iron.concurrency.core.execution;

import java.util.Objects;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 命名线程工厂。
 */
public class NamedThreadFactory implements ThreadFactory {

    private final String threadNamePrefix;
    private final boolean daemon;

    private final AtomicInteger index = new AtomicInteger(1);

    public NamedThreadFactory(String threadNamePrefix) {
        this(threadNamePrefix, false);
    }

    public NamedThreadFactory(String threadNamePrefix, boolean daemon) {
        this.threadNamePrefix = Objects.requireNonNull(threadNamePrefix, "threadNamePrefix must not be null");
        this.daemon = daemon;
    }

    @Override
    public Thread newThread(Runnable runnable) {
        Thread thread = new Thread(runnable);
        thread.setName(threadNamePrefix + index.getAndIncrement());
        thread.setDaemon(false);
        thread.setUncaughtExceptionHandler((t, e) -> {
            // 实现时用 logger 打印
        });
        return thread;
    }
}
