package com.xjtu.iron.foundation.time;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * 描述一个对象的创建时间和存活时间。
 */
public final class Expiration {

    /** 对象创建或开始生效的时间点。 */
    private final Instant createdAt;
    /** 对象允许存活的持续时间。 */
    private final Duration ttl;

    public Expiration(Instant createdAt, Duration ttl) {
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        if (ttl == null || ttl.isNegative()) {
            throw new IllegalArgumentException("ttl must not be null or negative");
        }
        this.ttl = ttl;
    }

    public Instant expiresAt() {
        return createdAt.plus(ttl);
    }

    public boolean isExpired(ClockProvider clockProvider) {
        return !clockProvider.now().isBefore(expiresAt());
    }

    public Duration remaining(ClockProvider clockProvider) {
        Duration remaining = Duration.between(clockProvider.now(), expiresAt());
        return remaining.isNegative() ? Duration.ZERO : remaining;
    }
}
