package com.xjtu.iron.idempotent.api.operation.write;

import com.xjtu.iron.idempotent.api.policy.IdempotencyPolicy;
import com.xjtu.iron.idempotent.api.repository.write.IdempotencyFailureInfo;
import com.xjtu.iron.idempotent.api.storage.IdempotencyStorageContext;

/** FAILED 低层终态命令。 */
public final class IdempotencyFailureCommand {

    private final String key;
    private final IdempotencyStorageContext storageContext;
    private final String ownerToken;
    private final long version;
    private final IdempotencyFailureInfo failure;
    private final String policyName;
    private final IdempotencyPolicy policy;

    private IdempotencyFailureCommand(Builder builder) {
        this.key = builder.key;
        this.storageContext = builder.storageContext;
        this.ownerToken = builder.ownerToken;
        this.version = builder.version;
        this.failure = builder.failure;
        this.policyName = builder.policyName;
        this.policy = builder.policy;
    }

    public static Builder builder() { return new Builder(); }

    public String getKey() { return key; }
    public IdempotencyStorageContext getStorageContext() { return storageContext; }
    public String getOwnerToken() { return ownerToken; }
    public long getVersion() { return version; }
    public IdempotencyFailureInfo getFailure() { return failure; }
    public String getPolicyName() { return policyName; }
    public IdempotencyPolicy getPolicy() { return policy; }

    public static final class Builder {
        private String key;
        private IdempotencyStorageContext storageContext;
        private String ownerToken;
        private long version;
        private IdempotencyFailureInfo failure;
        private String policyName;
        private IdempotencyPolicy policy;

        public Builder key(String value) { this.key = value; return this; }
        public Builder storageContext(IdempotencyStorageContext value) { this.storageContext = value; return this; }
        public Builder ownerToken(String value) { this.ownerToken = value; return this; }
        public Builder version(long value) { this.version = value; return this; }
        public Builder failure(IdempotencyFailureInfo value) { this.failure = value; return this; }
        public Builder policyName(String value) { this.policyName = value; return this; }
        public Builder policy(IdempotencyPolicy value) { this.policy = value; return this; }
        public IdempotencyFailureCommand build() { return new IdempotencyFailureCommand(this); }
    }
}
