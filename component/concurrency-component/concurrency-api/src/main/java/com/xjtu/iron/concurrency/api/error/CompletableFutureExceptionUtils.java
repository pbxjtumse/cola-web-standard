package com.xjtu.iron.concurrency.api.error;


import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

/**
 * CompletableFuture 异常工具类。
 *
 * <p>
 * CompletableFuture 在 join/get 时经常会包一层 CompletionException 或 ExecutionException，
 * 该工具类用于剥离包装异常，拿到真实异常。
 * </p>
 */
public final class CompletableFutureExceptionUtils {

    private CompletableFutureExceptionUtils() {
    }

    /**
     * 剥离 CompletionException 和 ExecutionException。
     *
     * @param throwable 原始异常
     * @return 解包后的异常
     */
    public static Throwable unwrap(Throwable throwable) {
        if (throwable == null) {
            return null;
        }

        Throwable current = throwable;

        while (current instanceof CompletionException || current instanceof ExecutionException) {
            if (current.getCause() == null) {
                return current;
            }
            current = current.getCause();
        }

        return current;
    }

    /**
     * 获取根因异常。
     *
     * @param throwable 原始异常
     * @return 根因异常
     */
    public static Throwable rootCause(Throwable throwable) {
        Throwable current = unwrap(throwable);

        if (current == null) {
            return null;
        }

        while (current.getCause() != null) {
            current = current.getCause();
        }

        return current;
    }
}
