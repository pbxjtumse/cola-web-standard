package com.xjtu.iron.idempotent.api.repository;

import com.xjtu.iron.idempotent.api.IdempotencyStatus;

import java.time.Instant;

/**
 * Repository 中的一条不可变幂等状态快照。
 *
 * <p>三个字段是并发正确性的核心：</p>
 * <ul>
 *     <li>{@code ownerToken}：当前 PROCESSING 执行者身份；</li>
 *     <li>{@code version}：每次重新抢占递增，可作为 fencing version；</li>
 *     <li>{@code processingExpireAt}：当前执行权租约截止时间。</li>
 * </ul>
 */
public final class IdempotencyRecord {

    private final String namespace;
    private final String key;
    private final String requestHash;
    private final IdempotencyStatus status;
    private final String ownerToken;
    private final long version;
    private final String resultPayload;
    private final String failureCode;
    private final String failureMessage;
    private final boolean failureRetryable;
    private final Instant processingExpireAt;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final Instant completedAt;

    private IdempotencyRecord(Builder builder) {
        this.namespace = builder.namespace;
        this.key = builder.key;
        this.requestHash = builder.requestHash;
        this.status = builder.status;
        this.ownerToken = builder.ownerToken;
        this.version = builder.version;
        this.resultPayload = builder.resultPayload;
        this.failureCode = builder.failureCode;
        this.failureMessage = builder.failureMessage;
        this.failureRetryable = builder.failureRetryable;
        this.processingExpireAt = builder.processingExpireAt;
        this.createdAt = builder.createdAt;
        this.updatedAt = builder.updatedAt;
        this.completedAt = builder.completedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getNamespace() {
        return namespace;
    }

    public String getKey() {
        return key;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public IdempotencyStatus getStatus() {
        return status;
    }

    public String getOwnerToken() {
        return ownerToken;
    }

    public long getVersion() {
        return version;
    }

    public String getResultPayload() {
        return resultPayload;
    }

    public String getFailureCode() {
        return failureCode;
    }

    public String getFailureMessage() {
        return failureMessage;
    }

    public boolean isFailureRetryable() {
        return failureRetryable;
    }

    public Instant getProcessingExpireAt() {
        return processingExpireAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public static final class Builder {
        private String namespace;
        private String key;
        private String requestHash;
        private IdempotencyStatus status;
        private String ownerToken;
        private long version;
        private String resultPayload;
        private String failureCode;
        private String failureMessage;
        private boolean failureRetryable;
        private Instant processingExpireAt;
        private Instant createdAt;
        private Instant updatedAt;
        private Instant completedAt;

        public Builder namespace(String value) {
            this.namespace = value;
            return this;
        }

        public Builder key(String value) {
            this.key = value;
            return this;
        }

        public Builder requestHash(String value) {
            this.requestHash = value;
            return this;
        }

        public Builder status(IdempotencyStatus value) {
            this.status = value;
            return this;
        }

        public Builder ownerToken(String value) {
            this.ownerToken = value;
            return this;
        }

        public Builder version(long value) {
            this.version = value;
            return this;
        }

        public Builder resultPayload(String value) {
            this.resultPayload = value;
            return this;
        }

        public Builder failureCode(String value) {
            this.failureCode = value;
            return this;
        }

        public Builder failureMessage(String value) {
            this.failureMessage = value;
            return this;
        }

        public Builder failureRetryable(boolean value) {
            this.failureRetryable = value;
            return this;
        }

        public Builder processingExpireAt(Instant value) {
            this.processingExpireAt = value;
            return this;
        }

        public Builder createdAt(Instant value) {
            this.createdAt = value;
            return this;
        }

        public Builder updatedAt(Instant value) {
            this.updatedAt = value;
            return this;
        }

        public Builder completedAt(Instant value) {
            this.completedAt = value;
            return this;
        }

        public IdempotencyRecord build() {
            return new IdempotencyRecord(this);
        }
    }
}
