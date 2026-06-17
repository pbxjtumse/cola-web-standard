package com.xjtu.iron.concurrency.api.enums;

/**
 * 线程池拒绝策略。
 */
public enum RejectionPolicy {

    /**
     * 拒绝感知版 ABORT。
     *
     * <p>任务标记为 REJECTED、Future 异常完成，并向提交方同步抛出拒绝异常。推荐默认策略。</p>
     */
    ABORT,

    /**
     * 调用方线程执行任务。
     * 线程池未关闭：任务由提交线程直接执行 不标记为 REJECTED  最终仍然是 RUNNING -> SUCCESS / FAILED
     * 线程池已关闭：任务标记为 REJECTED 抛出拒绝异常
     * <p>可能拖慢 Tomcat / RPC 线程，使用要谨慎。</p>
     */
    CALLER_RUNS,

    /**
     * 直接丢弃新任务。【拒绝感知版 DISCARD】
     * 业务任务不执行-> 状态标记为 REJECTED
     * Future 异常完成 -> 同步提交方收到拒绝异常
     * <p>一般不建议业务使用。</p>
     */
    DISCARD,

    /**
     * 丢弃队列中最老的任务，然后尝试提交新任务。
     */
    DISCARD_OLDEST,
    /**
     * 线程池饱和 -> 提交线程等待队列空位
     * -> 等待时间内有空位：进入队列
     * -> 等待超时：REJECTED
     * -> 线程中断：恢复中断标记并 REJECTED
     * <p>组件增强策略，不是 JDK 原生策略。</p>
     *
     * <p>该策略需要谨慎，因为它会阻塞提交线程 需要配置 rejectionWaitTime。</p>
     */
    BLOCKING_WAIT
}
