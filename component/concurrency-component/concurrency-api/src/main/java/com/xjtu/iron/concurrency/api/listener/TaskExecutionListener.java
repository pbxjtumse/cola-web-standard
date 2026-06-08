package com.xjtu.iron.concurrency.api.listener;

import com.xjtu.iron.concurrency.api.event.TaskExecutionEvent;

/**
 * 任务执行监听器。
 *
 * <p>用于扩展日志、审计、指标、任务状态中心、治理组件联动等能力。</p>
 */
public interface TaskExecutionListener {

    /** 任务提交到线程池之前触发。 */
    default void onSubmitted(TaskExecutionEvent event) {
    }

    /** 任务真正开始执行时触发。 */
    default void onStarted(TaskExecutionEvent event) {
    }

    /** 任务成功完成时触发。 */
    default void onSuccess(TaskExecutionEvent event) {
    }

    /** 任务执行失败时触发。 */
    default void onFailure(TaskExecutionEvent event) {
    }

    /** 任务被线程池拒绝时触发。 */
    default void onRejected(TaskExecutionEvent event) {
    }

    /** 任务排队超时或结果层超时时触发。 */
    default void onTimeout(TaskExecutionEvent event) {
    }

    /** fallback 被触发时触发。 */
    default void onFallback(TaskExecutionEvent event) {
    }

    /** 任务进入最终状态时触发。 */
    default void onCompleted(TaskExecutionEvent event) {
    }
}
