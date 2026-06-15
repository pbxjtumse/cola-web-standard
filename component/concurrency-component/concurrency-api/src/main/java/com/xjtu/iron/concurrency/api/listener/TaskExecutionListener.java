package com.xjtu.iron.concurrency.api.listener;

import com.xjtu.iron.concurrency.api.event.TaskExecutionEvent;

/**
 * 任务执行监听器。
 *
 * <p>
 * 业务方可以实现该接口，监听异步任务从提交、执行到最终结束的完整生命周期。
 * 所有方法均提供默认空实现，业务方只需要覆盖自己关心的阶段。
 * </p>
 *
 * <p>
 * {@code onCompleted} 是通用最终完成通知，不替代 {@code onSuccess}、
 * {@code onFailure}、{@code onTimeout} 等具体状态通知。
 * </p>
 */
public interface TaskExecutionListener {

    /**
     * 任务成功提交给线程池前后触发的提交事件。
     *
     * @param event 提交事件
     */
    default void onSubmitted(TaskExecutionEvent event) {
    }

    /**
     * 任务被工作线程取出并真正开始执行时触发。
     *
     * @param event 开始执行事件
     */
    default void onStarted(TaskExecutionEvent event) {
    }

    /**
     * 原始任务逻辑正常返回时触发。
     *
     * @param event 原始任务成功事件
     */
    default void onSuccess(TaskExecutionEvent event) {
    }

    /**
     * 原始任务逻辑抛出异常时触发。
     *
     * @param event 原始任务失败事件
     */
    default void onFailure(TaskExecutionEvent event) {
    }

    /**
     * 任务在提交阶段被线程池拒绝时触发。
     *
     * @param event 拒绝事件
     */
    default void onRejected(TaskExecutionEvent event) {
    }

    /**
     * 任务发生排队超时或结果层超时时触发。
     *
     * <p>
     * 具体是排队超时还是等待结果超时，可通过
     * {@code event.getError().getClassification().getStage()} 和
     * {@code getReason()} 判断。
     * </p>
     *
     * @param event 超时事件
     */
    default void onTimeout(TaskExecutionEvent event) {
    }

    /**
     * 任务被主动取消时触发。
     *
     * @param event 取消事件
     */
    default void onCancelled(TaskExecutionEvent event) {
    }

    /**
     * 原始任务异常后，fallback 即将开始执行时触发。
     *
     * @param event fallback 触发事件
     */
    default void onFallback(TaskExecutionEvent event) {
    }

    /**
     * fallback 正常返回降级值时触发。
     *
     * @param event fallback 成功事件
     */
    default void onFallbackSuccess(TaskExecutionEvent event) {
    }

    /**
     * fallback 自身也抛出异常时触发。
     *
     * @param event fallback 失败事件
     */
    default void onFallbackFailure(TaskExecutionEvent event) {
    }

    /**
     * 整个任务结果管道进入最终状态时触发。
     *
     * <p>
     * 无 fallback 时，最终状态通常是 SUCCESS、FAILED、REJECTED、TIMEOUT 或 CANCELLED；
     * 配置 fallback 时，最终状态通常是 FALLBACK_SUCCESS 或 FALLBACK_FAILED。
     * </p>
     *
     * @param event 最终状态事件
     */
    default void onCompleted(TaskExecutionEvent event) {
    }
}
