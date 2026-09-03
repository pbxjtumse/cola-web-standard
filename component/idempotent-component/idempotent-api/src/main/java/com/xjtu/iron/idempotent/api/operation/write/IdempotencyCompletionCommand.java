package com.xjtu.iron.idempotent.api.operation.write;

import com.xjtu.iron.idempotent.api.policy.IdempotencyPolicy;
import com.xjtu.iron.idempotent.api.storage.IdempotencyStorageContext;

/**
 * SUCCESS / DISCARDED 共用的低层终态命令。
 *
 * <p>调用方必须继续携带 acquire 时的 ownerToken + version；它们共同证明“我仍然是当前 generation”。</p>
 */
public final class IdempotencyCompletionCommand {

    private final String key;
    private final IdempotencyStorageContext storageContext;
    private final String ownerToken;
    private final long version;
    private final String resultPayload;
    private final String policyName;
    private final IdempotencyPolicy policy;

    private IdempotencyCompletionCommand(Builder builder) {
        this.key = builder.key;
        this.storageContext = builder.storageContext;
        this.ownerToken = builder.ownerToken;
        this.version = builder.version;
        this.resultPayload = builder.resultPayload;
        this.policyName = builder.policyName;
        this.policy = builder.policy;
    }

    public static Builder builder() { return new Builder(); }

    public String getKey() { return key; }
    public IdempotencyStorageContext getStorageContext() { return storageContext; }
    public String getOwnerToken() { return ownerToken; }
    public long getVersion() { return version; }
    public String getResultPayload() { return resultPayload; }
    public String getPolicyName() { return policyName; }
    public IdempotencyPolicy getPolicy() { return policy; }

    public static final class Builder {
        private String key;
        private IdempotencyStorageContext storageContext;
        private String ownerToken;
        private long version;
        private String resultPayload;
        private String policyName;
        private IdempotencyPolicy policy;

        public Builder key(String value) { this.key = value; return this; }
        public Builder storageContext(IdempotencyStorageContext value) { this.storageContext = value; return this; }
        public Builder ownerToken(String value) { this.ownerToken = value; return this; }
        public Builder version(long value) { this.version = value; return this; }
        public Builder resultPayload(String value) { this.resultPayload = value; return this; }
        public Builder policyName(String value) { this.policyName = value; return this; }
        public Builder policy(IdempotencyPolicy value) { this.policy = value; return this; }
        public IdempotencyCompletionCommand build() { return new IdempotencyCompletionCommand(this); }
    }
}
