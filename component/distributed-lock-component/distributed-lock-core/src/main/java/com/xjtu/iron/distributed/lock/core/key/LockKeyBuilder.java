package com.xjtu.iron.distributed.lock.core.key;

import com.xjtu.iron.distributed.lock.api.LockOptions;

/**
 * 锁 key 构造器。
 *
 * <p>负责把业务 lockName 转换为底层存储使用的真实 lockKey。统一构造 key 可以避免不同业务直接拼 key
 * 导致命名混乱，也便于后续做指标归一化。</p>
 */
public interface LockKeyBuilder {

    /**
     * 构造底层锁 key。
     *
     * @param lockName 业务锁名称。
     * @param options  锁选项。
     * @return 底层锁 key。
     */
    String buildLockKey(String lockName, LockOptions options);

    /**
     * 构造 fencing token key。
     *
     * @param lockName 业务锁名称。
     * @param options  锁选项。
     * @return fencing token key。
     */
    String buildFencingKey(String lockName, LockOptions options);
}
