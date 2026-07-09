package com.xjtu.iron.distributed.lock.core.spi;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Provider 解锁请求。
 *
 * <p>该对象由 LockLease 转换而来，并携带解锁原因、是否发布释放通知等扩展信息。使用 request 对象而不是直接
 * 传 LockLease，可以避免后续扩展 release 行为时破坏 Provider SPI。</p>
 */
public final class LockReleaseRequest {

    private final String namespace;
    private final String lockName;
    private final String lockKey;
    private final String ownerToken;
    private final String releaseReason;
    private final boolean publishReleaseEvent;
    private final Map<String, String> attributes;

    private LockReleaseRequest(Builder builder) {
        this.namespace = requireText(builder.namespace, "namespace");
        this.lockName = requireText(builder.lockName, "lockName");
        this.lockKey = requireText(builder.lockKey, "lockKey");
        this.ownerToken = requireText(builder.ownerToken, "ownerToken");
        this.releaseReason = builder.releaseReason;
        this.publishReleaseEvent = builder.publishReleaseEvent;
        this.attributes = Collections.unmodifiableMap(new LinkedHashMap<>(builder.attributes));
    }

    public static Builder builder() {
        return new Builder();
    }

    public static LockReleaseRequest fromLease(LockLease lease) {
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
    public String getReleaseReason() { return releaseReason; }
    public boolean isPublishReleaseEvent() { return publishReleaseEvent; }
    public Map<String, String> getAttributes() { return attributes; }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    /** LockReleaseRequest 构造器。 */
    public static final class Builder {
        private String namespace;
        private String lockName;
        private String lockKey;
        private String ownerToken;
        private String releaseReason = "normal";
        private boolean publishReleaseEvent = true;
        private Map<String, String> attributes = new LinkedHashMap<>();

        private Builder() {}

        public Builder namespace(String namespace) { this.namespace = namespace; return this; }
        public Builder lockName(String lockName) { this.lockName = lockName; return this; }
        public Builder lockKey(String lockKey) { this.lockKey = lockKey; return this; }
        public Builder ownerToken(String ownerToken) { this.ownerToken = ownerToken; return this; }
        public Builder releaseReason(String releaseReason) { this.releaseReason = releaseReason; return this; }
        public Builder publishReleaseEvent(boolean publishReleaseEvent) { this.publishReleaseEvent = publishReleaseEvent; return this; }
        public Builder attributes(Map<String, String> attributes) { this.attributes = attributes == null ? new LinkedHashMap<>() : new LinkedHashMap<>(attributes); return this; }
        public LockReleaseRequest build() { return new LockReleaseRequest(this); }
    }
}
