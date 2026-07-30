package com.xjtu.iron.retry.api;

import java.util.Objects;
import java.util.concurrent.Callable;

/**
 * 进程内同步重试执行入口。
 */
public interface RetryExecutor {

    /**
     * 使用直接提供的策略执行一个可重试操作。
     */
    <T> RetryResult<T> execute(
            String operationName,
            RetryOperation<T> operation,
            RetryPolicy retryPolicy);

    /**
     * 使用注册表中的命名策略执行一个可重试操作。
     */
    <T> RetryResult<T> execute(
            String operationName,
            RetryOperation<T> operation,
            String policyName);

    default <T> RetryResult<T> execute(RetryOperation<T> operation, RetryPolicy retryPolicy) {
        return execute("anonymous", operation, retryPolicy);
    }

    default <T> RetryResult<T> execute(
            String operationName,
            Callable<T> callable,
            RetryPolicy retryPolicy) {
        Objects.requireNonNull(callable, "callable must not be null");
        return execute(operationName, context -> callable.call(), retryPolicy);
    }

    default <T> RetryResult<T> execute(
            String operationName,
            Callable<T> callable,
            String policyName) {
        Objects.requireNonNull(callable, "callable must not be null");
        return execute(operationName, context -> callable.call(), policyName);
    }

    default RetryResult<Void> run(
            String operationName,
            Runnable runnable,
            RetryPolicy retryPolicy) {
        Objects.requireNonNull(runnable, "runnable must not be null");
        return execute(operationName, context -> {
            runnable.run();
            return null;
        }, retryPolicy);
    }

    default RetryResult<Void> run(
            String operationName,
            Runnable runnable,
            String policyName) {
        Objects.requireNonNull(runnable, "runnable must not be null");
        return execute(operationName, context -> {
            runnable.run();
            return null;
        }, policyName);
    }

    default <T> T executeAndGet(
            String operationName,
            RetryOperation<T> operation,
            RetryPolicy retryPolicy) {
        return execute(operationName, operation, retryPolicy).getOrThrow();
    }

    default <T> T executeAndGet(
            String operationName,
            RetryOperation<T> operation,
            String policyName) {
        return execute(operationName, operation, policyName).getOrThrow();
    }
}
