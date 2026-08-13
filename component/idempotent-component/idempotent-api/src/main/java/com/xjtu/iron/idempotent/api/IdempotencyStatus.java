package com.xjtu.iron.idempotent.api;

/**
 * Repository 中真正持久化的幂等状态。
 *
 * <p>V1.1 刻意只保留三态。PROCESSING_EXPIRED、FAILED_RETRYABLE、REPLAYED 等
 * 都属于“派生判定”或“一次调用结果”，不应该污染数据库 status 字段。</p>
 */
public enum IdempotencyStatus {

    /** 当前存在合法 owner/version，业务尚未完成。 */
    PROCESSING,

    /** 业务已经成功完成；可选保存 resultPayload 用于重复请求回放。 */
    SUCCESS,

    /** 本次 generation 已失败；是否允许恢复由 failureRetryable 单独描述。 */
    FAILED
}
