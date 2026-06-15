package com.xjtu.iron.concurrency.core.lifecycle;

import com.xjtu.iron.concurrency.api.event.TaskExecutionEvent;

/**
 * 任务生命周期发布器。
 *
 * <p>
 * 统一负责把 TaskExecutionEvent 分发给指标记录器、任务状态注册表和任务监听器，
 * 避免 TaskCommand 与结果管道重复编写三套分发代码。
 * </p>
 */
public interface TaskLifecyclePublisher {

    /**
     * 发布一次任务状态变化。
     *
     * @param event 任务执行事件
     */
    void publish(TaskExecutionEvent event);

    /**
     * 发布任务最终完成事件。
     *
     * <p>
     * completed 是通用终态通知，不替代 SUCCESS、FAILED、TIMEOUT 等具体状态事件。
     * </p>
     *
     * @param terminalEvent 最终状态事件
     */
    void publishCompleted(TaskExecutionEvent terminalEvent);
}
