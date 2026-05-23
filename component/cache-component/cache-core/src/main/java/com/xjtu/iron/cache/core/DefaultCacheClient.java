package com.xjtu.iron.cache.core;


import com.xjtu.iron.cache.api.CacheClient;
import com.xjtu.iron.cache.api.CacheKey;
import com.xjtu.iron.cache.api.CacheLoader;
import com.xjtu.iron.cache.api.CacheResult;
import com.xjtu.iron.cache.api.CacheSpec;
import com.xjtu.iron.cache.api.CacheValue;
import com.xjtu.iron.cache.api.enums.CacheDegradePolicy;
import com.xjtu.iron.cache.api.enums.CacheNullPolicy;
import com.xjtu.iron.cache.api.enums.CacheOperation;
import com.xjtu.iron.cache.api.exception.CacheException;
import com.xjtu.iron.cache.api.exception.CacheLoadException;

import java.time.Duration;
import java.util.Optional;

public class DefaultCacheClient implements CacheClient {

    private final CacheProvider cacheProvider;
    private final CacheSpecResolver specResolver;
    private final CacheTtlResolver ttlResolver;
    private final CacheLoadGuard loadGuard;
    private final CacheMetricsRecorder metricsRecorder;

    public DefaultCacheClient(
            CacheProvider cacheProvider,
            CacheSpecResolver specResolver,
            CacheTtlResolver ttlResolver,
            CacheLoadGuard loadGuard,
            CacheMetricsRecorder metricsRecorder
    ) {
        this.cacheProvider = cacheProvider;
        this.specResolver = specResolver;
        this.ttlResolver = ttlResolver;
        this.loadGuard = loadGuard;
        this.metricsRecorder = metricsRecorder;
    }

    @Override
    public <T> CacheResult<T> get(CacheKey key, Class<T> valueType, CacheLoader<T> loader) {
        CacheSpec spec = specResolver.resolve(key);
        return get(key, valueType, spec, loader);
    }

    @Override
    public <T> CacheResult<T> get(CacheKey key, Class<T> valueType, CacheSpec spec, CacheLoader<T> loader) {
        long start = System.currentTimeMillis();

        CacheValue<T> cacheValue;

        try {
            cacheValue = cacheProvider.get(key, valueType, spec);
        } catch (Exception ex) {
            metricsRecorder.recordError(key, CacheOperation.GET, ex);
            return handleCacheReadException(key, spec, loader, start, ex);
        }

        if (cacheValue.isPresent()) {
            long cost = System.currentTimeMillis() - start;
            metricsRecorder.recordHit(key, cacheValue.getLevel(), cost);
            return CacheResult.hit(cacheValue, cost);
        }

        metricsRecorder.recordMiss(key, System.currentTimeMillis() - start);

        try {
            if (spec.isMutexLoad()) {
                return loadWithMutex(key, valueType, spec, loader, start);
            }

            return loadAndWrite(key, spec, loader, start);
        } catch (Exception ex) {
            if (ex instanceof CacheException cacheException) {
                throw cacheException;
            }

            throw new CacheLoadException("Cache load failed, key=" + key.fullKey(), ex);
        }
    }

    private <T> CacheResult<T> loadWithMutex(
            CacheKey key,
            Class<T> valueType,
            CacheSpec spec,
            CacheLoader<T> loader,
            long start
    ) throws Exception {
        return loadGuard.execute(key.fullKey(), () -> {
            CacheValue<T> recheckValue = cacheProvider.get(key, valueType, spec);

            if (recheckValue.isPresent()) {
                long cost = System.currentTimeMillis() - start;
                metricsRecorder.recordHit(key, recheckValue.getLevel(), cost);
                return CacheResult.hit(recheckValue, cost);
            }

            return loadAndWrite(key, spec, loader, start);
        });
    }

    private <T> CacheResult<T> loadAndWrite(
            CacheKey key,
            CacheSpec spec,
            CacheLoader<T> loader,
            long start
    ) {
        T loadedValue;

        try {
            loadedValue = loader.load();
        } catch (Exception ex) {
            metricsRecorder.recordError(key, CacheOperation.GET, ex);
            throw new CacheLoadException("Cache loader failed, key=" + key.fullKey(), ex);
        }

        try {
            if (loadedValue == null) {
                if (spec.getNullPolicy() == CacheNullPolicy.CACHE_NULL) {
                    Duration nullTtl = ttlResolver.resolveNullValueTtl(spec);
                    cacheProvider.putNullValue(key, nullTtl, spec);
                }
            } else {
                Duration ttl = ttlResolver.resolveNormalTtl(spec);
                cacheProvider.put(key, loadedValue, ttl, spec);
            }
        } catch (Exception ex) {
            metricsRecorder.recordError(key, CacheOperation.PUT, ex);
        }

        long cost = System.currentTimeMillis() - start;
        metricsRecorder.recordLoad(key, cost);

        return CacheResult.loaded(loadedValue, cost);
    }

    private <T> CacheResult<T> handleCacheReadException(
            CacheKey key,
            CacheSpec spec,
            CacheLoader<T> loader,
            long start,
            Exception ex
    ) {
        CacheDegradePolicy degradePolicy = spec.getDegradePolicy();

        if (degradePolicy == CacheDegradePolicy.RETURN_NULL) {
            return CacheResult.degraded(null, System.currentTimeMillis() - start);
        }

        if (degradePolicy == CacheDegradePolicy.LOAD_SOURCE) {
            try {
                T value = loader.load();
                return CacheResult.degraded(value, System.currentTimeMillis() - start);
            } catch (Exception loadEx) {
                throw new CacheLoadException("Cache degraded load source failed, key=" + key.fullKey(), loadEx);
            }
        }

        throw new CacheException("Cache get failed, key=" + key.fullKey(), ex);
    }

    @Override
    public <T> Optional<T> getIfPresent(CacheKey key, Class<T> valueType) {
        CacheSpec spec = specResolver.resolve(key);
        CacheValue<T> value = cacheProvider.get(key, valueType, spec);

        if (!value.isPresent() || value.isNullValue()) {
            return Optional.empty();
        }

        return Optional.ofNullable(value.getValue());
    }

    @Override
    public void put(CacheKey key, Object value) {
        CacheSpec spec = specResolver.resolve(key);

        Duration ttl = value == null
                ? ttlResolver.resolveNullValueTtl(spec)
                : ttlResolver.resolveNormalTtl(spec);

        putInternal(key, value, ttl, spec);
    }

    @Override
    public void put(CacheKey key, Object value, Duration ttl) {
        CacheSpec spec = specResolver.resolve(key);
        putInternal(key, value, ttl, spec);
    }

    @Override
    public void put(CacheKey key, Object value, CacheSpec spec) {
        Duration ttl = value == null
                ? ttlResolver.resolveNullValueTtl(spec)
                : ttlResolver.resolveNormalTtl(spec);

        putInternal(key, value, ttl, spec);
    }

    @Override
    public void evict(CacheKey key) {
        try {
            cacheProvider.evict(key);
        } catch (Exception ex) {
            metricsRecorder.recordError(key, CacheOperation.EVICT, ex);
            throw new CacheException("Cache evict failed, key=" + key.fullKey(), ex);
        }
    }

    @Override
    public void refresh(CacheKey key, Class<?> valueType, CacheLoader<?> loader) {
        long start = System.currentTimeMillis();

        try {
            Object value = loader.load();
            put(key, value);
            metricsRecorder.recordLoad(key, System.currentTimeMillis() - start);

        } catch (Exception ex) {
            metricsRecorder.recordError(key, CacheOperation.REFRESH, ex);
            throw new CacheLoadException("Cache refresh failed, key=" + key.fullKey(), ex);
        }
    }

    private void putInternal(CacheKey key, Object value, Duration ttl, CacheSpec spec) {
        try {
            if (value == null) {
                if (spec.getNullPolicy() == CacheNullPolicy.SKIP_NULL) {
                    return;
                }

                cacheProvider.putNullValue(key, ttl, spec);
                return;
            }

            cacheProvider.put(key, value, ttl, spec);

        } catch (Exception ex) {
            metricsRecorder.recordError(key, CacheOperation.PUT, ex);
            throw new CacheException("Cache put failed, key=" + key.fullKey(), ex);
        }
    }
}
