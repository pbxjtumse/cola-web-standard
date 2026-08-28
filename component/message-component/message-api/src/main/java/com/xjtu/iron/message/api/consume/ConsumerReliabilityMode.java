package com.xjtu.iron.message.api.consume;

/**
 * 消费可靠性模式。
 */
public enum ConsumerReliabilityMode {
    /** 尽力而为，适合日志、埋点等非核心消息。 */
    BEST_EFFORT,

    /** 至少一次，业务成功后再确认 Broker，可能重复投递。 */
    AT_LEAST_ONCE,

    /** 业务效果一次，依赖至少一次投递、消费幂等和本地事务边界。 */
    EFFECTIVELY_ONCE,

    /** Provider 原生 exactly-once 语义，v13 不作为主目标。 */
    PROVIDER_EXACTLY_ONCE
}
