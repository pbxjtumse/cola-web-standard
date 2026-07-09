package com.xjtu.iron.distributed.lock.core.event;

import com.xjtu.iron.distributed.lock.api.LockStatus;

import java.time.Instant;

/**
 * 分布式锁事件。
 *
 * <p>事件用于日志、审计、监听器和指标桥接。注意不要在事件中放入过大的业务对象。</p>
 */
public final class LockEvent {

    private final LockEventType eventType;
    private final String namespace;
    private final String lockName;
    private final String lockNamePattern;
    private final String lockKey;
    private final String providerName;
    private final String ownerToken;
    private final Long fencingToken;
    private final LockStatus status;
    private final Throwable error;
    private final String message;
    private final Instant timestamp;

    private LockEvent(Builder builder) {
        this.eventType = builder.eventType;
        this.namespace = builder.namespace;
        this.lockName = builder.lockName;
        this.lockNamePattern = builder.lockNamePattern;
        this.lockKey = builder.lockKey;
        this.providerName = builder.providerName;
        this.ownerToken = builder.ownerToken;
        this.fencingToken = builder.fencingToken;
        this.status = builder.status;
        this.error = builder.error;
        this.message = builder.message;
        this.timestamp = builder.timestamp == null ? Instant.now() : builder.timestamp;
        if (eventType == null) { throw new IllegalArgumentException("eventType must not be null"); }
    }

    public static Builder builder() { return new Builder(); }

    public LockEventType getEventType() { return eventType; }
    public String getNamespace() { return namespace; }
    public String getLockName() { return lockName; }
    public String getLockNamePattern() { return lockNamePattern; }
    public String getLockKey() { return lockKey; }
    public String getProviderName() { return providerName; }
    public String getOwnerToken() { return ownerToken; }
    public Long getFencingToken() { return fencingToken; }
    public LockStatus getStatus() { return status; }
    public Throwable getError() { return error; }
    public String getMessage() { return message; }
    public Instant getTimestamp() { return timestamp; }

    /** LockEvent 构造器。 */
    public static final class Builder {
        private LockEventType eventType;
        private String namespace;
        private String lockName;
        private String lockNamePattern;
        private String lockKey;
        private String providerName;
        private String ownerToken;
        private Long fencingToken;
        private LockStatus status;
        private Throwable error;
        private String message;
        private Instant timestamp;
        private Builder() {}
        public Builder eventType(LockEventType eventType) { this.eventType = eventType; return this; }
        public Builder namespace(String namespace) { this.namespace = namespace; return this; }
        public Builder lockName(String lockName) { this.lockName = lockName; return this; }
        public Builder lockNamePattern(String lockNamePattern) { this.lockNamePattern = lockNamePattern; return this; }
        public Builder lockKey(String lockKey) { this.lockKey = lockKey; return this; }
        public Builder providerName(String providerName) { this.providerName = providerName; return this; }
        public Builder ownerToken(String ownerToken) { this.ownerToken = ownerToken; return this; }
        public Builder fencingToken(Long fencingToken) { this.fencingToken = fencingToken; return this; }
        public Builder status(LockStatus status) { this.status = status; return this; }
        public Builder error(Throwable error) { this.error = error; return this; }
        public Builder message(String message) { this.message = message; return this; }
        public Builder timestamp(Instant timestamp) { this.timestamp = timestamp; return this; }
        public LockEvent build() { return new LockEvent(this); }
    }
}
