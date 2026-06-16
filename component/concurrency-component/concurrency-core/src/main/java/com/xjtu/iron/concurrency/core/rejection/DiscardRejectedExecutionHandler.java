package com.xjtu.iron.concurrency.core.rejection;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 拒绝感知版 Discard 策略。
 *
 * <p>
 * 任务本身不会执行，但组件会把任务状态更新为 REJECTED，并使对应 Future 异常完成。
 * 为了让 execute/tryExecute 能够同步知道提交失败，本实现会抛出拒绝异常，
 * 因此它不是 JDK DiscardPolicy 的“完全静默”语义。
 * </p>
 */
public final class DiscardRejectedExecutionHandler implements RejectedExecutionHandler {

    @Override
    public void rejectedExecution(Runnable runnable, ThreadPoolExecutor executor) {
        throw RejectedTaskSupport.reject(runnable, executor.isShutdown()
                        ? "Executor already shutdown"
                        : "Task discarded by DISCARD policy"
        );
    }
}
