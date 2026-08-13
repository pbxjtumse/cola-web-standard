package com.xjtu.iron.idempotent.api.repository;

/**
 * markSuccess / markFailed 的条件写结果。
 */
public enum IdempotencyWriteStatus {

    /** owner/version 条件命中并成功完成状态更新。 */
    UPDATED,

    /** 记录不存在。 */
    NOT_FOUND,

    /** 当前记录已被新的 owner/version 接管，旧执行者不能再写最终状态。 */
    STALE_OWNER,

    /** 记录已经处于 SUCCESS/FAILED 等不允许再次覆盖的状态。 */
    ALREADY_FINAL,

    /** Provider 访问异常。 */
    PROVIDER_ERROR
}
