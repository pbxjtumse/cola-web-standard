package com.xjtu.iron.cache.core;

import com.xjtu.iron.cache.api.CacheSpec;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

/**
 * TTL 解析器。
 *
 * <p>负责把 CacheSpec 中配置的 TTL 转换为本次写入真正使用的 TTL。</p>
 *
 * <p>一期支持 TTL 随机抖动，用于避免大量 key 同时过期造成缓存雪崩。</p>
 */
public class CacheTtlResolver {

    /**
     * 计算正常值 TTL。
     *
     * <p>如果 ttl=5m，ttlJitter=60s，那么最终 TTL 会在 4m~6m 之间随机波动。</p>
     */
    public Duration resolveNormalTtl(CacheSpec spec) {
        Duration ttl = safeDuration(spec.getTtl(), Duration.ofMinutes(10));
        Duration jitter = safeDuration(spec.getTtlJitter(), Duration.ZERO);

        if (jitter.isZero() || jitter.isNegative()) {
            return ttl;
        }

        long baseMillis = ttl.toMillis();
        long jitterMillis = jitter.toMillis();
        long offset = ThreadLocalRandom.current().nextLong(-jitterMillis, jitterMillis + 1);
        long finalMillis = Math.max(1000L, baseMillis + offset);
        return Duration.ofMillis(finalMillis);
    }

    /** 计算空值 TTL。空值 TTL 通常应明显短于正常值 TTL。 */
    public Duration resolveNullValueTtl(CacheSpec spec) {
        return safeDuration(spec.getNullValueTtl(), Duration.ofSeconds(30));
    }

    /** 如果配置为空、0 或负数，则使用默认值兜底。 */
    private Duration safeDuration(Duration duration, Duration defaultValue) {
        if (duration == null || duration.isZero() || duration.isNegative()) {
            return defaultValue;
        }
        return duration;
    }
}
