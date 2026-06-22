package com.xjtu.iron.concurrency.core.spi;

/**
 * 线程池立即关闭时，对尚未开始任务的通知协议。
 */
public interface ShutdownAbortAware {

    /**
     * 通知任务因线程池关闭而无法继续执行。
     *
     * @param cause 关闭原因
     */
    void abortOnShutdown(Throwable cause);
}
