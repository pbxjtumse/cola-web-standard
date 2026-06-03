package com.xjtu.iron.concurrency.api.enums;

/**
 * 线程池拒绝策略。
 */
public enum RejectionPolicy {

    /**
     * 直接抛出异常。
     *
     * <p>推荐默认策略。</p>
     */
    ABORT,

    /**
     * 调用方线程执行任务。
     *
     * <p>可能拖慢 Tomcat / RPC 线程，使用要谨慎。</p>
     */
    CALLER_RUNS,

    /**
     * 直接丢弃新任务。
     *
     * <p>一般不建议业务使用。</p>
     */
    DISCARD,

    /**
     * 丢弃队列中最老的任务，然后尝试提交新任务。
     */
    DISCARD_OLDEST,
    /**
     * 阻塞等待队列空位。
     *
     * <p>组件增强策略，不是 JDK 原生策略。</p>
     *
     * <p>需要配置 rejectionWaitTime。</p>
     */
    BLOCKING_WAIT
}
