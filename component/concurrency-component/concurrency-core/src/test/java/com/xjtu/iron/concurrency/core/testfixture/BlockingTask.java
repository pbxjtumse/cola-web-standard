package com.xjtu.iron.concurrency.core.testfixture;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public final class BlockingTask implements Supplier<String> {

    private final CountDownLatch started = new CountDownLatch(1);
    private final CountDownLatch release = new CountDownLatch(1);
    private final AtomicBoolean executed = new AtomicBoolean(false);
    private final AtomicBoolean interrupted = new AtomicBoolean(false);

    @Override
    public String get() {
        executed.set(true);
        started.countDown();

        try {
            release.await();
            return "released";
        } catch (InterruptedException e) {
            interrupted.set(true);
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    public void awaitStarted() {
        AwaitTestSupport.await(started, "task should start");
    }

    public void release() {
        release.countDown();
    }

    public boolean executed() {
        return executed.get();
    }

    public boolean interrupted() {
        return interrupted.get();
    }
}
