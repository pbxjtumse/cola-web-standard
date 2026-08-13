package com.xjtu.iron.idempotent.api.repository;

/**
 * 显式 {@code recover()} 调用 Repository.tryRecover() 后的判定结果。
 */
public enum IdempotencyRecoveryStatus {

    /** 成功接管旧 generation，生成新的 owner/version，可以执行恢复业务。 */
    RECOVERY_ACQUIRED,

    /** 记录已经 SUCCESS，不需要恢复。 */
    SUCCESS,

    /** PROCESSING 仍然有效，不能接管。 */
    PROCESSING_ACTIVE,

    /** recoveryMode/window/业务规则不允许恢复。 */
    NOT_RECOVERABLE,

    /** FAILED 但属于不可恢复失败。 */
    FAILED_FINAL,

    /** 记录已经不存在。 */
    NOT_FOUND,

    /** routeKey/requestHash 与当前记录冲突。 */
    KEY_CONFLICT,

    /** expectedOwnerToken / expectedVersion 与当前记录不一致，说明扫描任务已经过时。 */
    STALE_CANDIDATE,

    /** Provider 访问异常。 */
    PROVIDER_ERROR
}
