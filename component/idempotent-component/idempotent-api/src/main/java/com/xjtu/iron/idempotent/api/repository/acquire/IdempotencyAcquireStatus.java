package com.xjtu.iron.idempotent.api.repository.acquire;

/**
 * 普通 {@code execute()} 调用 Repository.tryAcquire() 后的判定结果。
 *
 * <p>PROCESSING_ACTIVE / PROCESSING_EXPIRED / FAILED_* 都是派生判定，不是数据库持久 status；
 * V2 持久状态为 PROCESSING / SUCCESS / FAILED / DISCARDED。</p>
 */
public enum IdempotencyAcquireStatus {

    /** 本次成功创建或开启一个新的 PROCESSING generation，可以执行业务。 */
    ACQUIRED,

    /** 已有 SUCCESS；Executor 应直接 replay，不再执行业务。 */
    SUCCESS,

    /** 已有 DISCARDED；上层应返回“历史已丢弃”语义，不再执行业务。 */
    DISCARDED,

    /** 已有 PROCESSING 且 processingExpireAt > now。 */
    PROCESSING_ACTIVE,

    /** 已有 PROCESSING 但执行权已过期；普通路径只返回，不自动接管。 */
    PROCESSING_EXPIRED,

    /** 已有 FAILED 且记录标记为可恢复。 */
    FAILED_RETRYABLE,

    /** 已有 FAILED 且不可恢复。 */
    FAILED_FINAL,

    /** 同 key 的 routeKey/requestHash 与已有 generation 不一致。 */
    KEY_CONFLICT,

    /** Provider 访问异常。 */
    PROVIDER_ERROR
}
