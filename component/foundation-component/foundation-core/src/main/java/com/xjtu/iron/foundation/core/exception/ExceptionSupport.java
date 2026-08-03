package com.xjtu.iron.foundation.core.exception;

import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

/**
 * 提供不包含具体组件语义的异常处理能力。
 */
public final class ExceptionSupport {

    private ExceptionSupport() {
    }

    /**
     * 获取异常原因链中最深层异常。
     */
    public static Throwable rootCause(Throwable throwable) {
        java.util.List<Throwable> chain = ThrowableChain.toList(throwable);
        return chain.isEmpty() ? null : chain.get(chain.size() - 1);
    }

    /**
     * 判断原因链中是否包含指定异常类型。
     */
    public static boolean contains(Throwable throwable, Class<? extends Throwable> type) {
        if (type == null) {
            return false;
        }
        return ThrowableChain.toList(throwable).stream().anyMatch(type::isInstance);
    }

    /**
     * 解包 CompletableFuture 和 Future 常见包装异常。
     */
    public static Throwable unwrapAsync(Throwable throwable) {
        Throwable current = throwable;
        while ((current instanceof CompletionException || current instanceof ExecutionException)
                && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    /**
     * 恢复线程中断标记，并返回原异常，便于调用方继续抛出。
     */
    public static InterruptedException restoreInterrupt(InterruptedException exception) {
        Thread.currentThread().interrupt();
        return exception;
    }
}
