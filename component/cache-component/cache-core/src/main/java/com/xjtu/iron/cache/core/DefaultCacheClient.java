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
 * 职责：默认缓存客户端实现。
 * 1. 根据 CacheKey 解析 CacheSpec
 * 2. 调用 CacheProvider 读取缓存
 * 3. 处理缓存 miss 后的 loader 加载
 * 4. 处理空值缓存
 * 5. 处理 TTL 和 TTL 抖动
 * 6. 处理本地互斥加载，防止单机缓存击穿
 * 7. 记录缓存指标
 * 8. 处理缓存读取异常后的降级策略
 *
 * 注意：
 * 1.DefaultCacheClient 不直接依赖 Caffeine 或 Redis 或者相关的工具类
 * 2.底层缓存实现通过 CacheProvider SPI 接入。
 * 3.一期默认 CacheProvider 是 CompositeCacheProvider。
 */
public class DefaultCacheClient implements CacheClient {
    /**
     * 真正执行缓存读写的 Provider。
     *
     * 一期默认是 CompositeCacheProvider：
     * L1 = Caffeine
     * L2 = Redis
     */
    private final CacheProvider cacheProvider;

    /**
     * 根据 cacheName 解析缓存策略。 根据 cacheName 解析对应的 CacheSpec
     *
     * 比如：
     * campaignRule -> TTL 5m，开启 L1/L2，开启空值缓存
     * userProfile  -> 只开启 Redis，不开启本地缓存
     */
    private final CacheSpecResolver specResolver;

    /**
     * 负责计算 TTL。
     *
     * 它会处理：
     * 1. 正常值 TTL
     * 2. 空值 TTL
     * 3. TTL 随机抖动，防止雪崩
     */
    private final CacheTtlResolver ttlResolver;
    /**
     * 防缓存击穿。
     *
     * 一期是本地锁，只能防单 JVM 内并发击穿。 防止同一个 JVM 内同一个 key 被并发加载
     * 二期可以换成 Redis 分布式锁。
     */
    private final CacheLoadGuard loadGuard;
    /**
     * 指标记录器。
     *
     * 默认 Noop。
     * 如果项目里有 Micrometer，就使用 MicrometerCacheMetricsRecorder。
     */
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
        // 1. 根据 key.cacheName() 找缓存策略
        CacheSpec spec = specResolver.resolve(key);
        // 2. 用找到的策略执行真正 get
        return get(key, valueType, spec, loader);
    }

    @Override
    public <T> CacheResult<T> get(CacheKey key, Class<T> valueType, CacheSpec spec, CacheLoader<T> loader) {
        long start = System.currentTimeMillis();

        CacheValue<T> cacheValue;

        try {
            // 1. 先查缓存。
            // 如果是 CompositeCacheProvider：
            // 先查 Caffeine，再查 Redis。
            cacheValue = cacheProvider.get(key, valueType, spec);
        } catch (Exception ex) {
            // 2. 如果缓存读取异常，比如 Redis 抖动，则记录异常指标
            metricsRecorder.recordError(key, CacheOperation.GET, ex);
            // 3. 根据降级策略处理：
            //    LOAD_SOURCE：直接查数据源
            //    RETURN_NULL：返回 null
            //    FAIL_FAST：直接抛异常
            return handleCacheReadException(key, spec, loader, start, ex);
        }

        if (cacheValue.isPresent()) {
            // 4. 缓存命中，直接返回
            long cost = System.currentTimeMillis() - start;
            metricsRecorder.recordHit(key, cacheValue.getLevel(), cost);
            return CacheResult.hit(cacheValue, cost);
        }

        // 5. 缓存未命中，记录 miss
        metricsRecorder.recordMiss(key, System.currentTimeMillis() - start);

        try {
            if (spec.isMutexLoad()) {
                // 6. 开启互斥加载，防止大量线程同时查数据库
                return loadWithMutex(key, valueType, spec, loader, start);
            }
            // 7. 不开启互斥加载，直接查数据源并写缓存
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
