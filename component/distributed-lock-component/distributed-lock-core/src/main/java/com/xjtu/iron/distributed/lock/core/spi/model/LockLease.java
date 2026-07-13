package com.xjtu.iron.distributed.lock.core.spi.model;

import java.time.Duration;
import java.time.Instant;
import java.util.OptionalLong;

/**
 * 锁租约数据快照。
 *
 * <p>该对象表示一次成功加锁后，Provider 返回的【不可变】租约信息。它只描述“锁是谁、什么时候拿到、租约多久、
 * 底层 key/path 是什么”，不保存 lost/released 等运行时状态。</p>
 *
 * <p>lost/released 是客户端运行态，后续应放在 DefaultLockHandle 或 LockRuntimeState 中，并使用 AtomicBoolean
 * 保护并发释放和失锁标记。</p>
 */
public final class LockLease {

    /** Provider 名称。 */
    private final String providerName;

    /** 锁命名空间。 */
    private final String namespace;

    /** 业务锁名称。 */
    private final String lockName;

    /** Provider 真实锁 key 或路径。 */
    private final String lockKey;

    /** ownerToken。 */
    private final String ownerToken;

    /** fencingToken，可为空。 */
    private final Long fencingToken;

    /** 租约时间。 */
    private final Duration leaseTime;

    /** 加锁成功时间。 */
    private final Instant acquiredAt;

    /** 本地估算过期时间。 */
    private final Instant expireAt;

    private LockLease(Builder builder) {
        this.providerName = requireText(builder.providerName, "providerName");
        this.namespace = requireText(builder.namespace, "namespace");
        this.lockName = requireText(builder.lockName, "lockName");
        this.lockKey = requireText(builder.lockKey, "lockKey");
        this.ownerToken = requireText(builder.ownerToken, "ownerToken");
        this.fencingToken = builder.fencingToken;
        this.leaseTime = requirePositive(builder.leaseTime, "leaseTime");
        this.acquiredAt = builder.acquiredAt == null ? Instant.now() : builder.acquiredAt;
        this.expireAt = builder.expireAt == null ? this.acquiredAt.plus(this.leaseTime) : builder.expireAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getProviderName() {
        return providerName;
    }

    public String getNamespace() {
        return namespace;
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

    public OptionalLong fencingToken() {
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

    private static String requireText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    private static Duration requirePositive(Duration value, String fieldName) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return value;
    }

    /** LockLease 构造器。 */
    public static final class Builder {

        private String providerName;
        private String namespace;
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

        public Builder namespace(String namespace) {
            this.namespace = namespace;
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
