package com.xjtu.iron.concurrency.api.enums.error;


/**
 * 异步错误恢复动作建议。
 *
 * <p>
 * 该枚举不代表并行组件一定会执行恢复动作，
 * 只是给治理组件、补偿系统、业务监听器提供结构化建议。
 * </p>
 */
public enum AsyncRecoveryAction {

    /**
     * 不需要恢复动作。
     */
    NONE,

    /**
     * 可以快速重试。
     *
     * <p>
     * 例如短暂网络抖动、瞬时 RPC 失败。
     * </p>
     */
    FAST_RETRY,

    /**
     * 可以延迟重试。
     *
     * <p>
     * 例如线程池拒绝、下游暂时不可用。
     * </p>
     */
    DELAY_RETRY,

    /**
     * 需要进入补偿系统。
     */
    COMPENSATE,

    /**
     * 需要人工处理。
     */
    MANUAL,

    /**
     * 可以忽略。
     */
    IGNORE,

    /**
     * 需要告警。
     */
    ALERT
}
