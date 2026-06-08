package com.xjtu.iron.concurrency.api.listener;

import com.xjtu.iron.concurrency.api.event.TaskExecutionEvent;

/**
 * 任务执行监听器。
 *
 * <p>业务系统可以实现这个接口，把任务生命周期事件接入日志、审计、指标、链路追踪或治理系统。</p>
 *
 * <p>所有方法都有默认空实现，业务只需要覆盖自己关心的阶段。</p>
 */
public interface TaskExecutionListener {

    /**
     * 任务已提交。
     *
     * @param event 任务事件
     */
    default void onSubmitted(TaskExecutionEvent event) {
    }

    /**
     * 任务开始执行。
     *
     * @param event 任务事件
     */
    default void onStarted(TaskExecutionEvent event) {
    }

    /**
     * 任务执行成功。
     *
     * @param event 任务事件
     */
    default void onSuccess(TaskExecutionEvent event) {
    }

    /**
     * 任务执行失败。
     *
     * @param event 任务事件
     */
    default void onFailure(TaskExecutionEvent event) {
    }

    /**
     * 任务被拒绝。
     *
     * @param event 任务事件
     */
    default void onRejected(TaskExecutionEvent event) {
    }

    /**
     * 任务超时。
     *
     * @param event 任务事件
     */
    default void onTimeout(TaskExecutionEvent event) {
    }

    /**
     * 任务执行 fallback。
     *
     * @param event 任务事件
     */
    default void onFallback(TaskExecutionEvent event) {
    }

    /**
     * 任务生命周期结束。
     *
     * @param event 任务事件
     */
    default void onCompleted(TaskExecutionEvent event) {
    }
}
