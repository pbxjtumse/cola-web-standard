package com.xjtu.iron.cache.provider.composite;

import com.xjtu.iron.cache.api.CacheKey;
import com.xjtu.iron.cache.api.CacheSpec;
import com.xjtu.iron.cache.api.CacheValue;
import com.xjtu.iron.cache.core.CacheProvider;

import java.time.Duration;

/**
 * 多级缓存组合 Provider。
 *
 * <p>一期默认组合：L1 Caffeine + L2 Redis。</p>
 */
public class CompositeCacheProvider implements CacheProvider {

    /** L1 Provider，当前是 Caffeine。 */
    private final CacheProvider l1Provider;

    /** L2 Provider，当前是 Redis。 */
    private final CacheProvider l2Provider;

    /** 创建组合 Provider。 */
    public CompositeCacheProvider(CacheProvider l1Provider, CacheProvider l2Provider) {
        this.l1Provider = l1Provider;
        this.l2Provider = l2Provider;
    }

    /** 返回 Provider 名称。 */
    @Override
    public String name() {
        return "composite";
    }

    /** 按 L1 -> L2 的顺序读取缓存。 */
    @Override
    public <T> CacheValue<T> get(CacheKey key, Class<T> valueType, CacheSpec spec) {
        if (spec.isEnableL1()) {
            CacheValue<T> l1Value = l1Provider.get(key, valueType, spec);
            if (l1Value.isPresent()) {
                return l1Value;
            }
        }

        if (spec.isEnableL2()) {
            CacheValue<T> l2Value = l2Provider.get(key, valueType, spec);
            if (l2Value.isPresent()) {
                backfillL1(key, l2Value, spec);
                return l2Value;
            }
        }

        return CacheValue.miss();
    }

    /** Redis 命中后回填 L1。 */
    private <T> void backfillL1(CacheKey key, CacheValue<T> value, CacheSpec spec) {
        if (!spec.isEnableL1()) {
            return;
        }
        if (value.isNullValue()) {
            l1Provider.putNullValue(key, spec.getNullValueTtl(), spec);
            return;
        }
        l1Provider.put(key, value.getValue(), spec.getTtl(), spec);
    }

    /** 写入正常值到 L2 和 L1。 */
    @Override
    public void put(CacheKey key, Object value, Duration ttl, CacheSpec spec) {
        if (spec.isEnableL2()) {
            l2Provider.put(key, value, ttl, spec);
        }
        if (spec.isEnableL1()) {
            l1Provider.put(key, value, ttl, spec);
        }
    }

    /** 写入空值占位到 L2 和 L1。 */
    @Override
    public void putNullValue(CacheKey key, Duration ttl, CacheSpec spec) {
        if (spec.isEnableL2()) {
            l2Provider.putNullValue(key, ttl, spec);
        }
        if (spec.isEnableL1()) {
            l1Provider.putNullValue(key, ttl, spec);
        }
    }

    /** 删除 L1 和 L2。 */
    @Override
    public void evict(CacheKey key) {
        l1Provider.evict(key);
        l2Provider.evict(key);
    }
}
