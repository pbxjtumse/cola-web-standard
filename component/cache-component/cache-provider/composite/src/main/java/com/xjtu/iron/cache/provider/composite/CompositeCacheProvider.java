package com.xjtu.iron.cache.provider.composite;

import com.xjtu.iron.cache.api.CacheKey;
import com.xjtu.iron.cache.api.CacheSpec;
import com.xjtu.iron.cache.api.CacheValue;
import com.xjtu.iron.cache.core.CacheProvider;

import java.time.Duration;

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