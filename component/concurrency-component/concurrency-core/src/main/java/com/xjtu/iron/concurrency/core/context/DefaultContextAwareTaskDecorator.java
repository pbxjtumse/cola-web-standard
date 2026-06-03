package com.xjtu.iron.concurrency.core.context;

import com.xjtu.iron.concurrency.api.context.ContextAwareTaskDecorator;
import com.xjtu.iron.concurrency.api.context.ContextPropagator;
import com.xjtu.iron.concurrency.api.context.ContextScope;
import com.xjtu.iron.concurrency.api.context.ContextSnapshot;

import java.util.concurrent.Callable;
import java.util.function.Supplier;

public class DefaultContextAwareTaskDecorator implements ContextAwareTaskDecorator {

    private final ContextPropagator contextPropagator;

    public DefaultContextAwareTaskDecorator(ContextPropagator contextPropagator) {
        this.contextPropagator = contextPropagator;
    }

    @Override
    public Runnable decorate(Runnable runnable) {
        ContextSnapshot snapshot = contextPropagator.capture();

        return () -> {
            try (ContextScope ignored = snapshot.restore()) {
                runnable.run();
            }
        };
    }

    @Override
    public <T> Callable<T> decorate(Callable<T> callable) {
        ContextSnapshot snapshot = contextPropagator.capture();

        return () -> {
            try (ContextScope ignored = snapshot.restore()) {
                return callable.call();
            }
        };
    }

    @Override
    public <T> Supplier<T> decorate(Supplier<T> supplier) {
        ContextSnapshot snapshot = contextPropagator.capture();

        return () -> {
            try (ContextScope ignored = snapshot.restore()) {
                return supplier.get();
            }
        };
    }
}
