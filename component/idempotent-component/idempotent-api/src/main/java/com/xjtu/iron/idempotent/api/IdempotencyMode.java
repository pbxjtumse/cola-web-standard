package com.xjtu.iron.idempotent.api;

/**
 * 幂等记录的生命周期模式。
 */
public enum IdempotencyMode {

    /**
     * 有限时间窗口去重。
     *
     * <p>典型场景：按钮连点、接口短时间重复提交、短周期任务去重。
     * 语义窗口结束后允许开启新的 generation，通常由 Redis Provider 承载。</p>
     */
    SHORT_TERM,

    /**
     * 长期业务幂等。
     *
     * <p>典型场景：订单、支付、退款、结算、重要消息消费。
     * 默认不依赖 TTL 自动删除，通常由 JDBC Provider 持久化。</p>
     */
    DURABLE
}
