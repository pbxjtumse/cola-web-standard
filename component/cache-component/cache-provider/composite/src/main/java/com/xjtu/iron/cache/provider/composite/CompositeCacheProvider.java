package com.xjtu.iron.cache.provider.composite;

import com.xjtu.iron.cache.api.CacheKey;
import com.xjtu.iron.cache.api.CacheSpec;
import com.xjtu.iron.cache.api.CacheValue;
import com.xjtu.iron.cache.core.CacheProvider;

import java.time.Duration;
/**
 * 多级缓存组合 Provider。
 *
 * <p>一期默认组合：</p>
 *
 * <pre>
 * L1 = Caffeine 本地缓存
 * L2 = Redis 分布式缓存
 * </pre>
 *
 * <p>读取顺序：</p>
 *
 * <pre>
 * 1. 查 L1
 * 2. L1 miss 查 L2
 * 3. L2 hit 回填 L1
 * 4. L1、L2 都 miss 返回 miss
 * </pre>
 *
 * <p>写入顺序：</p>
 *
 * <pre>
 * 1. 写 L2 Redis
 * 2. 写 L1 Caffeine
 * </pre>
 *
 * <p>删除顺序：</p>
 *
 * <pre>
 * 1. 删除 L1
 * 2. 删除 L2
 * </pre>
 */
public class CompositeCacheProvider implements CacheProvider {

    private final CacheProvider l1Provider;
    private final CacheProvider l2Provider;

    public CompositeCacheProvider(CacheProvider l1Provider, CacheProvider l2Provider) {
        this.l1Provider = l1Provider;
        this.l2Provider = l2Provider;
    }

    @Override
    public String name() {
        return "composite";
    }

    @Override
    public <T> CacheValue<T> get(CacheKey key, Class<T> valueType, CacheSpec spec) {
        // 1. 如果开启 L1，本地缓存优先
        if (spec.isEnableL1()) {
            CacheValue<T> l1Value = l1Provider.get(key, valueType, spec);

            if (l1Value.isPresent()) {
                return l1Value;
            }
        }
        // 2. L1 未命中，再查 L2 Redis
        if (spec.isEnableL2()) {
            CacheValue<T> l2Value = l2Provider.get(key, valueType, spec);

            if (l2Value.isPresent()) {
                // 3. Redis 命中后，回填本地缓存
                //    下次同一个实例再查，就可以直接命中 Caffeine
                backfillL1(key, l2Value, spec);
                return l2Value;
            }
        }

        return CacheValue.miss();
    }

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

    @Override
    public void put(CacheKey key, Object value, Duration ttl, CacheSpec spec) {
        if (spec.isEnableL2()) {
            l2Provider.put(key, value, ttl, spec);
        }

        if (spec.isEnableL1()) {
            l1Provider.put(key, value, ttl, spec);
        }
    }

    @Override
    public void putNullValue(CacheKey key, Duration ttl, CacheSpec spec) {
        if (spec.isEnableL2()) {
            l2Provider.putNullValue(key, ttl, spec);
        }

        if (spec.isEnableL1()) {
            l1Provider.putNullValue(key, ttl, spec);
        }
    }

    @Override
    public void evict(CacheKey key) {
        l1Provider.evict(key);
        l2Provider.evict(key);
    }
}