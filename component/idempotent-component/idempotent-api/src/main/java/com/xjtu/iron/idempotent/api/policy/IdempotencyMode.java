package com.xjtu.iron.idempotent.api.policy;

/**
 * 幂等记录的生命周期语义。
 */
public enum IdempotencyMode {

    /**
     * 有限时间窗口去重。
     *
     * <p>典型场景：按钮连点、接口短时间重复提交、短周期任务去重。
     * 语义窗口结束后允许开启新的 generation。Redis 是默认承载方式，但不是唯一实现。</p>
     */
    WINDOWED,

    /**
     * 长期业务幂等。
     *
     * <p>典型场景：订单、支付、退款、结算、重要消息消费。
     * 幂等事实不依赖短 TTL 自动失效，通常由 JDBC 等持久化存储承载。</p>
     */
    DURABLE;

    public boolean isWindowed() {
        return this == WINDOWED;
    }
}
