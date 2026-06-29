package com.xjtu.iron.concurrency.core.support;

import com.xjtu.iron.concurrency.core.rejection.AwareAbortRejectedExecutionHandler;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class TestExecutors {

    private TestExecutors() {
    }

    public static ThreadPoolExecutor fixed(String name, int size) {
        return new ThreadPoolExecutor(
                size,
                size,
                60,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                namedFactory(name),
                new AwareAbortRejectedExecutionHandler()
        );
    }

    public static ThreadPoolExecutor smallRejecting(String name) {
        return new ThreadPoolExecutor(
                1,
                1,
                60,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(1),
                namedFactory(name),
                new AwareAbortRejectedExecutionHandler()
        );
    }

    public static ScheduledThreadPoolExecutor scheduler(String name) {
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1, namedFactory(name));
        executor.setRemoveOnCancelPolicy(true);
        return executor;
    }

    private static ThreadFactory namedFactory(String prefix) {
        AtomicInteger index = new AtomicInteger();
        return runnable -> {
            Thread thread = new Thread(runnable);
            thread.setName(prefix + "-" + index.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }
}
