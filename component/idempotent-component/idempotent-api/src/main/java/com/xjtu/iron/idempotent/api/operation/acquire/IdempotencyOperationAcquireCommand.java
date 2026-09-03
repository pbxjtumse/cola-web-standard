package com.xjtu.iron.idempotent.api.operation.acquire;

import com.xjtu.iron.idempotent.api.policy.IdempotencyPolicy;
import com.xjtu.iron.idempotent.api.storage.IdempotencyStorageContext;

/**
 * 低层 acquire 命令。
 *
 * <p>ownerToken 由调用方生成并持有，后续 markSuccess/markFailed/markDiscarded 必须继续使用该 owner 与 acquire 返回的 version。</p>
 */
public final class IdempotencyOperationAcquireCommand {

    private final String key;
    private final String requestHash;
    private final String routeKey;
    private final IdempotencyStorageContext storageContext;
    private final String ownerToken;
    private final String policyName;
    private final IdempotencyPolicy policy;

    private IdempotencyOperationAcquireCommand(Builder builder) {
        this.key = builder.key;
        this.requestHash = builder.requestHash;
        this.routeKey = builder.routeKey;
        this.storageContext = builder.storageContext;
        this.ownerToken = builder.ownerToken;
        this.policyName = builder.policyName;
        this.policy = builder.policy;
    }

    public static Builder builder() { return new Builder(); }

    public String getKey() { return key; }
    public String getRequestHash() { return requestHash; }
    public String getRouteKey() { return routeKey; }
    public IdempotencyStorageContext getStorageContext() { return storageContext; }
    public String getOwnerToken() { return ownerToken; }
    public String getPolicyName() { return policyName; }
    public IdempotencyPolicy getPolicy() { return policy; }

    public static final class Builder {
        private String key;
        private String requestHash;
        private String routeKey;
        private IdempotencyStorageContext storageContext;
        private String ownerToken;
        private String policyName;
        private IdempotencyPolicy policy;

        public Builder key(String value) { this.key = value; return this; }
        public Builder requestHash(String value) { this.requestHash = value; return this; }
        public Builder routeKey(String value) { this.routeKey = value; return this; }
        public Builder storageContext(IdempotencyStorageContext value) { this.storageContext = value; return this; }
        public Builder ownerToken(String value) { this.ownerToken = value; return this; }
        public Builder policyName(String value) { this.policyName = value; return this; }
        public Builder policy(IdempotencyPolicy value) { this.policy = value; return this; }
        public IdempotencyOperationAcquireCommand build() { return new IdempotencyOperationAcquireCommand(this); }
    }
}
