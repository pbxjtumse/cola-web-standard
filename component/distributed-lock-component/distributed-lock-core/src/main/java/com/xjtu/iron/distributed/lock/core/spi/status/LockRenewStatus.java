package com.xjtu.iron.distributed.lock.core.spi.status;

/**
 * 底层Provider续期状态,是细粒度的结果。
 * RENEWED        -> 继续 HELD
 * NOT_FOUND      -> LOCK_LOST, stage = RENEW
 * NOT_OWNER      -> LOCK_LOST, stage = RENEW
 * PROVIDER_ERROR -> PROVIDER_ERROR, stage = RENEW
 */
public enum LockRenewStatus {

    /** 续期成功。 */
    RENEWED,

    /** 底层锁 key 已不存在。 */
    NOT_FOUND,

    /** 底层锁存在，但 ownerToken 不匹配。 */
    NOT_OWNER,

    /** Provider 执行异常。 */
    PROVIDER_ERROR
}
