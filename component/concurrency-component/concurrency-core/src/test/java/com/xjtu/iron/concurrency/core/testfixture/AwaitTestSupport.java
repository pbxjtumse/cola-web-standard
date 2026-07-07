package com.xjtu.iron.concurrency.core.testfixture;

import java.util.concurrent.*;

public final class AwaitTestSupport {

    private AwaitTestSupport() {
    }

    public static void await(CountDownLatch latch, String message) {
        try {
            if (!latch.await(1, TimeUnit.SECONDS)) {
                throw new AssertionError(message);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(message, e);
        }
    }

    public static <T> T join(CompletableFuture<T> future) {
        try {
            return future.get(1, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new AssertionError("Future should complete successfully", e);
        }
    }

    public static Throwable joinException(CompletableFuture<?> future) {
        try {
            future.get(1, TimeUnit.SECONDS);
            throw new AssertionError("Future should complete exceptionally");
        } catch (ExecutionException e) {
            return e.getCause();
        } catch (TimeoutException e) {
            throw new AssertionError("Future should not be pending", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted while waiting future", e);
        }
    }

    public static void assertDone(CompletableFuture<?> future) {
        if (!future.isDone()) {
            throw new AssertionError("Future should be done");
        }
    }

    public static void assertNotDone(CompletableFuture<?> future) {
        if (future.isDone()) {
            throw new AssertionError("Future should not be done");
        }
    }
}
