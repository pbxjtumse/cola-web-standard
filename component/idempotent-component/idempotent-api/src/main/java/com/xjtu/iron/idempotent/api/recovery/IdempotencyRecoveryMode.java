package com.xjtu.iron.idempotent.api.recovery;

/**
 * PROCESSING 超时或可重试 FAILED 的恢复方式。
 *
 * <p>注意：该枚举只描述“是否允许外部可靠任务恢复”，并不在幂等组件内部启动扫描器。</p>
 */
public enum IdempotencyRecoveryMode {

    /**
     * 不提供自动恢复语义。
     *
     * <p>普通请求发现 PROCESSING 已超时后只返回 PROCESSING_EXPIRED，
     * 不会自动接管；{@code recover(...)} 也会拒绝。</p>
     */
    NONE,

    /**
     * 允许外部 Reliable Task / 调度组件扫描候选记录并调用 recover(...)。
     */
    EXTERNAL_TASK
}
