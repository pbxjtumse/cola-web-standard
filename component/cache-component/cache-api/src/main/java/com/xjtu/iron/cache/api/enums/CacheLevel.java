package com.xjtu.iron.cache.api.enums;

/**
 * CacheLevel 枚举。
 *
 * <p>用于描述缓存组件中的固定策略或状态，避免业务代码使用散乱字符串。</p>
 */
public enum CacheLevel {
    /**
     * 一级缓存，当前默认是 Caffeine 本地缓存。
     */
    L1,
    /**
     * 二级缓存，当前默认是 Redis 分布式缓存。
     */
    L2,
    /**
     * 源数据层，表示缓存未命中后执行了 loader。
     */
    SOURCE,
    /**
     * 没有命中任何缓存层，或者发生降级后没有明确命中层级。
     */
    NONE
}
