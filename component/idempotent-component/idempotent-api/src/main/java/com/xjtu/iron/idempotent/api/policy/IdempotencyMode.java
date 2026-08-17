package com.xjtu.iron.idempotent.api.policy;

/**
 * 幂等记录的生命周期语义。
 *
 * <p>V1.3 使用 {@link #WINDOWED} 作为有限时间窗口幂等的正式名称，
 * 因为“短期”容易被误解成业务执行时间短，而真正含义是：
 * 在一个有限幂等窗口内，相同 key 被视为同一个逻辑请求。</p>
 */
public enum IdempotencyMode {

    /**
     * 有限时间窗口去重。
     *
     * <p>典型场景：按钮连点、接口短时间重复提交、短周期任务去重。
     * 语义窗口结束后允许开启新的 generation。Redis 是默认承载方式，但不是语义上的唯一实现。</p>
     */
    WINDOWED,

    /**
     * 长期业务幂等。
     *
     * <p>典型场景：订单、支付、退款、结算、重要消息消费。
     * 幂等事实不依赖短 TTL 自动失效，通常由 JDBC 等持久化存储承载。</p>
     */
    DURABLE,

    /**
     * V1.2 兼容名称。
     *
     * @deprecated 请使用 {@link #WINDOWED}。
     */
    @Deprecated
    SHORT_TERM;

    /**
     * WINDOWED 与历史 SHORT_TERM 在语义上等价。
     */
    public boolean isWindowed() {
        return this == WINDOWED || this == SHORT_TERM;
    }

    /**
     * 把历史 SHORT_TERM 统一归一成 WINDOWED，避免 Registry/Provider 内部维护两套语义。
     */
    public IdempotencyMode canonical() {
        return isWindowed() ? WINDOWED : DURABLE;
    }
}
