package com.xjtu.iron.distributed.lock.core.spi.status;

/**
 * 底层解锁状态。
 */
public enum LockReleaseStatus {

    /** 解锁成功。 */
    RELEASED,

    /** 底层锁 key 已不存在。通常表示锁已过期或已被释放，当前 handle 应视为失锁。 */
    NOT_FOUND,

    /** 底层锁存在，但 ownerToken 不匹配。通常表示锁已经被其他 owner 重新获取。 */
    NOT_OWNER,

    /** Provider 执行异常。 */
    PROVIDER_ERROR
}
