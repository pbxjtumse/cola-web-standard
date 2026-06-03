package com.xjtu.iron.concurrency.core.execution;

import com.xjtu.iron.concurrency.api.execution.AsyncBatchResult;
import com.xjtu.iron.concurrency.api.execution.AsyncTaskOutcome;
import com.xjtu.iron.concurrency.api.execution.AsyncTemplate;
import com.xjtu.iron.concurrency.api.execution.NamedFuture;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * 默认 CompletableFuture 编排模板。
 */
public class DefaultAsyncTemplate implements AsyncTemplate {

    @Override
    public <T> CompletableFuture<List<T>> allOf(Collection<CompletableFuture<T>> futures) {
        CompletableFuture<?>[] array = futures.toArray(new CompletableFuture[0]);

        return CompletableFuture.allOf(array)
                .thenApply(ignored -> futures.stream()
                        .map(CompletableFuture::join)
                        .toList());
    }

    @Override
    public <T> CompletableFuture<AsyncBatchResult<T>> allOfOutcome(Collection<NamedFuture<T>> futures) {
        List<CompletableFuture<AsyncTaskOutcome<T>>> wrapped = new ArrayList<>();

        for (NamedFuture<T> namedFuture : futures) {
            CompletableFuture<AsyncTaskOutcome<T>> outcomeFuture =
                    namedFuture.getFuture()
                            .thenApply(value -> AsyncTaskOutcome.success(namedFuture.getTaskName(), value))
                            .exceptionally(ex -> AsyncTaskOutcome.failure(namedFuture.getTaskName(), unwrap(ex)));
            wrapped.add(outcomeFuture);
        }

        CompletableFuture<?>[] array = wrapped.toArray(new CompletableFuture[0]);

        return CompletableFuture.allOf(array)
                .thenApply(ignored -> {
                    List<AsyncTaskOutcome<T>> outcomes = wrapped.stream()
                            .map(CompletableFuture::join)
                            .toList();

                    return new AsyncBatchResult<>(outcomes);
                });
    }

    @Override
    public <T> CompletableFuture<List<T>> allOfFailFast(Collection<CompletableFuture<T>> futures) {
        CompletableFuture<List<T>> result = new CompletableFuture<>();
        for (CompletableFuture<T> future : futures) {
            future.whenComplete((value, error) -> {
                if (error != null && !result.isDone()) {
                    result.completeExceptionally(unwrap(error));

                    /*
                     * 注意：
                     * cancel(true) 对 CompletableFuture.supplyAsync 提交的任务不一定能中断底层线程。
                     * 这里只是尽力取消尚未完成的 Future。
                     */
                    for (CompletableFuture<T> other : futures) {
                        other.cancel(true);
                    }
                }
            });
        }
        CompletableFuture<?>[] array = futures.toArray(new CompletableFuture[0]);
        CompletableFuture.allOf(array)
                .thenApply(ignored -> futures.stream()
                        .map(CompletableFuture::join)
                        .toList())
                .whenComplete((values, error) -> {
                    if (error != null) {
                        result.completeExceptionally(unwrap(error));
                    } else {
                        result.complete(values);
                    }
                });
        return result;
    }

    @Override
    public <T> CompletableFuture<T> anyOf(Collection<CompletableFuture<T>> futures) {
        CompletableFuture<?>[] array = futures.toArray(new CompletableFuture[0]);

        return CompletableFuture.anyOf(array).thenApply(value -> (T) value);
    }

    @Override
    public <T> CompletableFuture<T> withTimeout(CompletableFuture<T> future, Duration timeout) {
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            return future;
        }

        return future.orTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public <T> CompletableFuture<T> withFallback(CompletableFuture<T> future, Function<Throwable, T> fallback) {
        if (fallback == null) {
            return future;
        }

        return future.exceptionally(error -> fallback.apply(unwrap(error)));
    }

    private Throwable unwrap(Throwable error) {
        if (error == null) {
            return null;
        }

        if (error.getCause() != null) {
            return error.getCause();
        }

        return error;
    }
}