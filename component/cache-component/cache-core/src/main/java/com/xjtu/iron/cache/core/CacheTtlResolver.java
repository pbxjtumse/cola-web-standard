package com.xjtu.iron.cache.core;


import com.xjtu.iron.cache.api.CacheSpec;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

public class CacheTtlResolver {

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

    public Duration resolveNullValueTtl(CacheSpec spec) {
        return safeDuration(spec.getNullValueTtl(), Duration.ofSeconds(30));
    }

    private Duration safeDuration(Duration duration, Duration defaultValue) {
        if (duration == null || duration.isZero() || duration.isNegative()) {
            return defaultValue;
        }
        return duration;
    }
}
