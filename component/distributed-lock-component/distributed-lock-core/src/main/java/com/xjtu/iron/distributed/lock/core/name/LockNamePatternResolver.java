package com.xjtu.iron.distributed.lock.core.name;

/**
 * 锁名称归一化器。
 *
 * <p>该接口主要用于指标 tag 降基数。不要把完整 lockName 直接作为 Prometheus tag，例如
 * {@code settle:batch:20260708:000001} 会产生海量时间序列。可以把它归一化为 {@code settle:batch:*}。</p>
 */
public interface LockNamePatternResolver {

    /**
     * 将业务锁名称解析为低基数 pattern。
     *
     * @param lockName 业务锁名称。
     * @return 归一化后的锁名称 pattern。
     */
    String resolvePattern(String lockName);
}
