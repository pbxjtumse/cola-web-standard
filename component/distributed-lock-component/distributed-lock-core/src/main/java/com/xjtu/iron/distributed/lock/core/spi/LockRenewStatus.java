package com.xjtu.iron.distributed.lock.core.spi;

/**
 * 底层续期状态。
 */
public enum LockRenewStatus {

    /**
     * 续期成功。
     */
    RENEWED,

    /**
     * 底层锁 key 已不存在。
     */
    NOT_FOUND,

    /**
     * 底层锁存在，但 ownerToken 不匹配。
     */
    NOT_OWNER,

    /**
     * Provider 执行异常。
     */
    PROVIDER_ERROR
}
