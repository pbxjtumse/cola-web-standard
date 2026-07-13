package com.xjtu.iron.distributed.lock.core.spi.request;

import com.xjtu.iron.distributed.lock.core.spi.model.LockLease;

import java.time.Duration;

/**
 * Provider 续期请求。
 */
public final class LockRenewRequest {

    private final String namespace;
    private final String lockName;
    private final String lockKey;
    private final String ownerToken;
    private final Duration leaseTime;
    private final String renewReason;

    private LockRenewRequest(Builder builder) {
        this.namespace = requireText(builder.namespace, "namespace");
        this.lockName = requireText(builder.lockName, "lockName");
        this.lockKey = requireText(builder.lockKey, "lockKey");
        this.ownerToken = requireText(builder.ownerToken, "ownerToken");
        this.leaseTime = requirePositive(builder.leaseTime, "leaseTime");
        this.renewReason = builder.renewReason;
    }

    public static Builder builder() { return new Builder(); }

    public static LockRenewRequest fromLease(LockLease lease) {
        return builder()
                .namespace(lease.getNamespace())
                .lockName(lease.getLockName())
                .lockKey(lease.getLockKey())
                .ownerToken(lease.getOwnerToken())
                .leaseTime(lease.getLeaseTime())
                .build();
    }

    public String getNamespace() { return namespace; }
    public String getLockName() { return lockName; }
    public String getLockKey() { return lockKey; }
    public String getOwnerToken() { return ownerToken; }
    public Duration getLeaseTime() { return leaseTime; }
    public String getRenewReason() { return renewReason; }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) { throw new IllegalArgumentException(fieldName + " must not be blank"); }
        return value.trim();
    }
    private static Duration requirePositive(Duration value, String fieldName) {
        if (value == null || value.isZero() || value.isNegative()) { throw new IllegalArgumentException(fieldName + " must be positive"); }
        return value;
    }

    /** LockRenewRequest 构造器。 */
    public static final class Builder {
        private String namespace;
        private String lockName;
        private String lockKey;
        private String ownerToken;
        private Duration leaseTime;
        private String renewReason = "watchdog";
        private Builder() {}
        public Builder namespace(String namespace) { this.namespace = namespace; return this; }
        public Builder lockName(String lockName) { this.lockName = lockName; return this; }
        public Builder lockKey(String lockKey) { this.lockKey = lockKey; return this; }
        public Builder ownerToken(String ownerToken) { this.ownerToken = ownerToken; return this; }
        public Builder leaseTime(Duration leaseTime) { this.leaseTime = leaseTime; return this; }
        public Builder renewReason(String renewReason) { this.renewReason = renewReason; return this; }
        public LockRenewRequest build() { return new LockRenewRequest(this); }
    }
}
