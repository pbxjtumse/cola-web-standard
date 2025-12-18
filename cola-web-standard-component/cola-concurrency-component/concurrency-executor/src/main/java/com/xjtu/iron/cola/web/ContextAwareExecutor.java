package com.xjtu.iron.cola.web;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class ContextAwareExecutor {

    private final ExecutorService delegate;

    public ContextAwareExecutor(ExecutorService delegate) {
        this.delegate = delegate;
    }

    public void execute(Runnable task) {
        delegate.execute(TaskWrapper.wrap(task));
    }

    public <T> Future<T> submit(Callable<T> task) {
        return delegate.submit(() -> {
            long start = System.currentTimeMillis();
            try {
                return task.call();
            } finally {
                //log.debug("Callable cost {} ms", System.currentTimeMillis() - start);
            }
        });
    }
}

