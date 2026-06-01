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

/**
 * 默认缓存客户端实现。
 *
 * <p>它是一期缓存组件的流程编排核心，但它不直接依赖 Caffeine 或 Redis。
 * 具体缓存能力通过 CacheProvider SPI 注入。</p>
 */
public class DefaultCacheClient implements CacheClient {

    /** 真正执行缓存读写的 Provider。一期默认是 CompositeCacheProvider。 */
    private final CacheProvider cacheProvider;

    /** 根据 cacheName 解析缓存策略。 */
    private final CacheSpecResolver specResolver;

    /** 负责计算正常值 TTL、空值 TTL 和 TTL 抖动。 */
    private final CacheTtlResolver ttlResolver;

    /** 防缓存击穿的加载保护器。一期默认是本地互斥锁。 */
    private final CacheLoadGuard loadGuard;

    /** 指标记录器。可由 Noop 或 Micrometer 实现。 */
    private final CacheMetricsRecorder metricsRecorder;

    /** 创建默认缓存客户端。 */
    public DefaultCacheClient(CacheProvider cacheProvider, CacheSpecResolver specResolver, CacheTtlResolver ttlResolver,
                              CacheLoadGuard loadGuard, CacheMetricsRecorder metricsRecorder) {
        this.cacheProvider = cacheProvider;
        this.specResolver = specResolver;
        this.ttlResolver = ttlResolver;
        this.loadGuard = loadGuard;
        this.metricsRecorder = metricsRecorder;
    }

    /** 根据 cacheName 默认策略读取缓存。 */
    @Override
    public <T> CacheResult<T> get(CacheKey key, Class<T> valueType, CacheLoader<T> loader) {
        CacheSpec spec = specResolver.resolve(key);
        return get(key, valueType, spec, loader);
    }

    /** 使用指定策略读取缓存。 */
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

    /**
     * 互斥加载源数据。
     *
     * <p>拿到锁后会二次查询缓存，避免其他线程已经完成加载并回填缓存后，本线程仍重复查源。</p>
     */
    private <T> CacheResult<T> loadWithMutex(CacheKey key, Class<T> valueType, CacheSpec spec, CacheLoader<T> loader, long start) throws Exception {
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

    /** 执行 loader，并根据返回值写入缓存。 */
    private <T> CacheResult<T> loadAndWrite(CacheKey key, CacheSpec spec, CacheLoader<T> loader, long start) {
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

    /** 处理缓存读取异常后的降级策略。 */
    private <T> CacheResult<T> handleCacheReadException(CacheKey key, CacheSpec spec, CacheLoader<T> loader, long start, Exception ex) {
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

    /** 只查缓存，不触发 loader。 */
    @Override
    public <T> Optional<T> getIfPresent(CacheKey key, Class<T> valueType) {
        CacheSpec spec = specResolver.resolve(key);
        CacheValue<T> value = cacheProvider.get(key, valueType, spec);
        if (!value.isPresent() || value.isNullValue()) {
            return Optional.empty();
        }
        return Optional.ofNullable(value.getValue());
    }

    /** 使用默认策略写入缓存。 */
    @Override
    public void put(CacheKey key, Object value) {
        CacheSpec spec = specResolver.resolve(key);
        Duration ttl = value == null ? ttlResolver.resolveNullValueTtl(spec) : ttlResolver.resolveNormalTtl(spec);
        putInternal(key, value, ttl, spec);
    }

    /** 使用显式 TTL 写入缓存。 */
    @Override
    public void put(CacheKey key, Object value, Duration ttl) {
        CacheSpec spec = specResolver.resolve(key);
        putInternal(key, value, ttl, spec);
    }

    /** 使用显式策略写入缓存。 */
    @Override
    public void put(CacheKey key, Object value, CacheSpec spec) {
        Duration ttl = value == null ? ttlResolver.resolveNullValueTtl(spec) : ttlResolver.resolveNormalTtl(spec);
        putInternal(key, value, ttl, spec);
    }

    /** put 系列方法的统一内部实现。 */
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

    /** 删除缓存。 */
    @Override
    public void evict(CacheKey key) {
        try {
            cacheProvider.evict(key);
        } catch (Exception ex) {
            metricsRecorder.recordError(key, CacheOperation.EVICT, ex);
            throw new CacheException("Cache evict failed, key=" + key.fullKey(), ex);
        }
    }

    /** 主动刷新缓存。 */
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
}
