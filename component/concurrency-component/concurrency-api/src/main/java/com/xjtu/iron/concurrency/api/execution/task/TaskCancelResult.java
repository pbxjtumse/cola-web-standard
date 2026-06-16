package com.xjtu.iron.concurrency.api.execution.task;

/**
 * 任务取消结果。
 */
public enum TaskCancelResult {

    /**
     * 取消成功，组件已经把任务最终状态确定为 CANCELLED。
     */
    CANCELLED,

    /**
     * 任务已经进入 SUCCESS、FAILED、TIMEOUT、REJECTED、CANCELLED 或 fallback 最终状态，
     * 当前取消请求没有改变任务结果。
     */
    ALREADY_COMPLETED,

    /**
     * 当前节点不存在该 taskId 对应的可取消运行实例。
     *
     * <p>任务可能从未提交、已经完成并被移除，或者运行在其他节点。</p>
     */
    NOT_FOUND
}
