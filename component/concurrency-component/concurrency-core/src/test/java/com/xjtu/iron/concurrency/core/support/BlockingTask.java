package com.xjtu.iron.concurrency.core.support;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 测试用可控阻塞任务：用于制造 timeout、cancel、线程池饱和等场景。
 */
public final class BlockingTask<T> {

    private final CountDownLatch started = new CountDownLatch(1);
    private final CountDownLatch release = new CountDownLatch(1);
    private final T value;
    private final AtomicBoolean interrupted = new AtomicBoolean(false);

    public BlockingTask(T value) {
        this.value = value;
    }

    public T get() {
        started.countDown();
        try {
            release.await();
            return value;
        } catch (InterruptedException interruptedException) {
            interrupted.set(true);
            Thread.currentThread().interrupt();
            throw new RuntimeException("blocking task interrupted", interruptedException);
        }
    }

    public void awaitStarted() throws InterruptedException {
        if (!started.await(2, TimeUnit.SECONDS)) {
            throw new AssertionError("task did not start in time");
        }
    }

    public void release() {
        release.countDown();
    }

    public boolean wasInterrupted() {
        return interrupted.get();
    }
}
