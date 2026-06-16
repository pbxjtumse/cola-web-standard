package com.xjtu.iron.concurrency.core.rejection;

import com.xjtu.iron.concurrency.core.task.RejectedTaskAware;

import java.util.concurrent.RejectedExecutionException;

/**
 * 拒绝任务通知工具。
 *
 * <p>
 * JDK 的拒绝策略只关心 Runnable 是否被接受；并行组件还需要把拒绝结果同步到TaskCommand 对应的 CompletableFuture、任务状态注册表、指标和监听器。
 * </p>
 */
final class RejectedTaskSupport {

    private RejectedTaskSupport() {
    }

    /**
     * 创建拒绝异常并通知支持拒绝感知的任务。
     *
     * @param runnable 被拒绝的任务
     * @param message 拒绝说明
     * @return 已完成拒绝通知的异常对象
     */
    static RejectedExecutionException reject(Runnable runnable, String message) {
        return reject(runnable, message, null);
    }

    /**
     * 创建拒绝异常并通知支持拒绝感知的任务。
     *
     * @param runnable 被拒绝的任务
     * @param message 拒绝说明
     * @param cause 原始异常
     * @return 已完成拒绝通知的异常对象
     */
    static RejectedExecutionException reject(Runnable runnable, String message, Throwable cause) {
        RejectedExecutionException exception = cause == null
                ? new RejectedExecutionException(message)
                : new RejectedExecutionException(message, cause);

        if (runnable instanceof RejectedTaskAware rejectedTaskAware) {
            try {
                rejectedTaskAware.reject(exception);
            } catch (Throwable notificationError) {
                /*
                 * 拒绝状态通知失败不能覆盖最初的拒绝语义，使用 suppressed 保留诊断信息。
                 */
                exception.addSuppressed(notificationError);
            }
        }

        return exception;
    }
}
