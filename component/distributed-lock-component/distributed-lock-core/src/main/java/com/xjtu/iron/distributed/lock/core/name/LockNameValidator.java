package com.xjtu.iron.distributed.lock.core.name;

/**
 * 锁名称校验器。
 *
 * <p>统一校验 lockName 可以避免业务方传入空字符串、非法分隔符、超长 key 等问题。Provider 可以在此基础上
 * 再做自己底层存储相关的 key/path 约束。</p>
 */
public interface LockNameValidator {

    /**
     * 校验业务锁名称。
     *
     * @param lockName 业务锁名称。
     */
    void validate(String lockName);
}
