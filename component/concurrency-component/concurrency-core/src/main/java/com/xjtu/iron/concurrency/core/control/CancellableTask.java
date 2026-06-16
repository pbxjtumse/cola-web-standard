package com.xjtu.iron.concurrency.core.control;

import com.xjtu.iron.concurrency.api.execution.task.TaskCancelResult;

/**
 * core 内部可取消任务控制对象。
 */
public interface CancellableTask {

    /**
     * 取消当前任务。
     *
     * @param mayInterruptIfRunning 是否尽力中断运行线程
     * @return 取消结果
     */
    TaskCancelResult cancel(boolean mayInterruptIfRunning);
}
