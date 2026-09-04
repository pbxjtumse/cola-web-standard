package com.xjtu.iron.idempotent.api.state;

/**
 * Repository 中真正持久化的幂等状态。
 *
 * <p>V2 正式保留四个持久状态：PROCESSING / SUCCESS / FAILED / DISCARDED。
 * PROCESSING_EXPIRED、FAILED_RETRYABLE、REPLAYED 等仍属于“派生判定”或“一次调用结果”，
 * 不应该继续扩散到数据库 status 字段。</p>
 */
public enum IdempotencyStatus {

    /** 当前存在合法 owner/version，业务尚未完成。 */
    PROCESSING,

    /** 业务已经成功完成；可选保存 resultPayload 用于重复请求回放。 */
    SUCCESS,

    /** 本次 generation 已失败；是否允许恢复由 failureRetryable 单独描述。 */
    FAILED,

    /**
     * 当前 generation 被明确终止并丢弃，后续重复请求不得再次执行业务。
     *
     * <p>与 SUCCESS 一样属于终态，但语义不同：SUCCESS 表示业务真实成功，
     * DISCARDED 表示业务明确决定“不再处理”。消息消费中的 DISCARD 就属于这一类。</p>
     */
    DISCARDED
}
