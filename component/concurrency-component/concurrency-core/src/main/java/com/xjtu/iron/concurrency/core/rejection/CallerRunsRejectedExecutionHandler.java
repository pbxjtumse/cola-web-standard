package com.xjtu.iron.concurrency.core.rejection;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 调用方线程执行拒绝策略。
 *
 * <p>
 * 线程池仍处于运行状态时，由提交任务的线程直接执行 Runnable；线程池已经关闭时，
 * 任务会被标记为拒绝并抛出 RejectedExecutionException。
 * </p>
 */
public final class CallerRunsRejectedExecutionHandler implements RejectedExecutionHandler {

    @Override
    public void rejectedExecution(Runnable runnable, ThreadPoolExecutor executor) {
        if (executor.isShutdown()) {
            throw RejectedTaskSupport.reject(runnable, "Executor already shutdown");
        }
        runnable.run();
    }
}
