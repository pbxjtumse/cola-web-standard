package com.xjtu.iron.distributed.lock.core.spi.request;

import com.xjtu.iron.distributed.lock.core.spi.model.LockLease;

/**
 * Provider 持锁检查请求。
 */
public final class LockCheckRequest {

    private final String namespace;
    private final String lockName;
    private final String lockKey;
    private final String ownerToken;

    private LockCheckRequest(Builder builder) {
        this.namespace = requireText(builder.namespace, "namespace");
        this.lockName = requireText(builder.lockName, "lockName");
        this.lockKey = requireText(builder.lockKey, "lockKey");
        this.ownerToken = requireText(builder.ownerToken, "ownerToken");
    }

    public static Builder builder() { return new Builder(); }

    public static LockCheckRequest fromLease(LockLease lease) {
        return builder()
                .namespace(lease.getNamespace())
                .lockName(lease.getLockName())
                .lockKey(lease.getLockKey())
                .ownerToken(lease.getOwnerToken())
                .build();
    }

    public String getNamespace() { return namespace; }
    public String getLockName() { return lockName; }
    public String getLockKey() { return lockKey; }
    public String getOwnerToken() { return ownerToken; }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) { throw new IllegalArgumentException(fieldName + " must not be blank"); }
        return value.trim();
    }

    /** LockCheckRequest 构造器。 */
    public static final class Builder {
        private String namespace;
        private String lockName;
        private String lockKey;
        private String ownerToken;
        private Builder() {}
        public Builder namespace(String namespace) { this.namespace = namespace; return this; }
        public Builder lockName(String lockName) { this.lockName = lockName; return this; }
        public Builder lockKey(String lockKey) { this.lockKey = lockKey; return this; }
        public Builder ownerToken(String ownerToken) { this.ownerToken = ownerToken; return this; }
        public LockCheckRequest build() { return new LockCheckRequest(this); }
    }
}
