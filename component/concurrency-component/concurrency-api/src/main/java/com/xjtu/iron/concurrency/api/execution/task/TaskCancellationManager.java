package com.xjtu.iron.concurrency.api.execution.task;

/**
 * 任务取消管理器。
 *
 * <p>
 * 一期实现只控制当前 JVM 节点上的运行任务。后续分布式任务控制需要增加节点路由、
 * Redis/DB 控制记录或消息通知，不应把本地实现误认为跨节点取消。
 * </p>
 */
public interface TaskCancellationManager {

    /**
     * 根据 taskId 取消当前节点任务。
     *
     * @param taskId 任务唯一 ID
     * @param mayInterruptIfRunning 是否尽力中断运行线程
     * @return 取消结果
     */
    TaskCancelResult cancel(String taskId, boolean mayInterruptIfRunning);

    /**
     * 判断当前节点是否还持有该任务的运行控制对象。
     *
     * @param taskId 任务唯一 ID
     * @return 是否仍可尝试取消
     */
    boolean isCancellable(String taskId);
}
