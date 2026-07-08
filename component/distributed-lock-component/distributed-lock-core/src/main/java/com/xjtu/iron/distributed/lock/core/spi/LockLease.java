package com.xjtu.iron.distributed.lock.core.spi;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.OptionalLong;

/**
 * Core 层内部锁租约。
 *
 * <p>LockLease 是 Provider 与 core 之间传递的内部对象，表示一次加锁成功后的底层租约。
 * API 层暴露给业务的是 {@code LockHandle}，不是本类。</p>
 */
public final class LockLease {

    /**
     * Provider 名称。
     */
    private final String providerName;

    /**
     * 业务锁名称。
     */
    private final String lockName;

    /**
     * 底层真实锁 key。
     */
    private final String lockKey;

    /**
     * 本次租约 ownerToken。
     */
    private final String ownerToken;

    /**
     * 本次租约 fencingToken。
     */
    private final Long fencingToken;

    /**
     * 租约时长。
     */
    private final Duration leaseTime;

    /**
     * 加锁成功时间。
     */
    private final Instant acquiredAt;

    /**
     * 本地估算过期时间。
     */
    private final Instant expireAt;

    /**
     * 是否已经判定失锁。
     */
    private volatile boolean lost;

    /**
     * 是否已经释放。
     */
    private volatile boolean released;

    private LockLease(Builder builder) {
        this.providerName = requireText(builder.providerName, "providerName");
        this.lockName = requireText(builder.lockName, "lockName");
        this.lockKey = requireText(builder.lockKey, "lockKey");
        this.ownerToken = requireText(builder.ownerToken, "ownerToken");
        this.fencingToken = builder.fencingToken;
        this.leaseTime = Objects.requireNonNull(builder.leaseTime, "leaseTime must not be null");
        this.acquiredAt = builder.acquiredAt == null ? Instant.now() : builder.acquiredAt;
        this.expireAt = builder.expireAt == null ? this.acquiredAt.plus(this.leaseTime) : builder.expireAt;
        if (leaseTime.isZero() || leaseTime.isNegative()) {
            throw new IllegalArgumentException("leaseTime must be positive");
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    private static String requireText(String value, String name) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }

    /**
     * 标记租约已经失锁。
     */
    public void markLost() {
        this.lost = true;
    }

    /**
     * 标记租约已经释放。
     */
    public void markReleased() {
        this.released = true;
    }

    /**
     * 根据本地时间估算租约是否已过期。
     *
     * @return 当前时间晚于 expireAt 返回 true。
     */
    public boolean isExpiredLocally() {
        return Instant.now().isAfter(expireAt);
    }

    public String getProviderName() {
        return providerName;
    }

    public String getLockName() {
        return lockName;
    }

    public String getLockKey() {
        return lockKey;
    }

    public String getOwnerToken() {
        return ownerToken;
    }

    public OptionalLong getFencingToken() {
        return fencingToken == null ? OptionalLong.empty() : OptionalLong.of(fencingToken);
    }

    public Duration getLeaseTime() {
        return leaseTime;
    }

    public Instant getAcquiredAt() {
        return acquiredAt;
    }

    public Instant getExpireAt() {
        return expireAt;
    }

    public boolean isLost() {
        return lost;
    }

    public boolean isReleased() {
        return released;
    }

    /**
     * LockLease 构造器。
     */
    public static final class Builder {

        private String providerName;
        private String lockName;
        private String lockKey;
        private String ownerToken;
        private Long fencingToken;
        private Duration leaseTime;
        private Instant acquiredAt;
        private Instant expireAt;

        private Builder() {
        }

        public Builder providerName(String providerName) {
            this.providerName = providerName;
            return this;
        }

        public Builder lockName(String lockName) {
            this.lockName = lockName;
            return this;
        }

        public Builder lockKey(String lockKey) {
            this.lockKey = lockKey;
            return this;
        }

        public Builder ownerToken(String ownerToken) {
            this.ownerToken = ownerToken;
            return this;
        }

        public Builder fencingToken(Long fencingToken) {
            this.fencingToken = fencingToken;
            return this;
        }

        public Builder leaseTime(Duration leaseTime) {
            this.leaseTime = leaseTime;
            return this;
        }

        public Builder acquiredAt(Instant acquiredAt) {
            this.acquiredAt = acquiredAt;
            return this;
        }

        public Builder expireAt(Instant expireAt) {
            this.expireAt = expireAt;
            return this;
        }

        public LockLease build() {
            return new LockLease(this);
        }
    }
}
