package com.xjtu.iron.idempotent.api.recovery;

import com.xjtu.iron.idempotent.api.policy.IdempotencyPolicy;

/**
 * 外部 Reliable Task 调用 recover() 时使用的请求。
 *
 * <p>真正关键的是 {@code expectedOwnerToken + expectedVersion}：扫描 candidate 只能说明“扫描当时看到 A/10 需要恢复”，
 * 真正执行任务时仍必须再次由 Repository 校验 current 是否还是 A/10。若已经变成 B/11，则返回 STALE_CANDIDATE。</p>
 */
public final class IdempotencyRecoveryRequest {

    private final String key;
    private final String requestHash;
    private final String routeKey;
    private final String expectedOwnerToken;
    private final Long expectedVersion;
    private final String policyName;
    private final IdempotencyPolicy policy;

    private IdempotencyRecoveryRequest(Builder builder) {
        this.key = builder.key;
        this.requestHash = builder.requestHash;
        this.routeKey = builder.routeKey;
        this.expectedOwnerToken = builder.expectedOwnerToken;
        this.expectedVersion = builder.expectedVersion;
        this.policyName = builder.policyName;
        this.policy = builder.policy;
    }

    public static Builder builder() { return new Builder(); }

    public String getKey() { return key; }
    public String getRequestHash() { return requestHash; }
    public String getRouteKey() { return routeKey; }
    public String getExpectedOwnerToken() { return expectedOwnerToken; }
    public Long getExpectedVersion() { return expectedVersion; }
    public String getPolicyName() { return policyName; }
    public IdempotencyPolicy getPolicy() { return policy; }

    public static final class Builder {
        private String key;
        private String requestHash;
        private String routeKey;
        private String expectedOwnerToken;
        private Long expectedVersion;
        private String policyName;
        private IdempotencyPolicy policy;

        public Builder key(String value) { this.key = value; return this; }
        public Builder requestHash(String value) { this.requestHash = value; return this; }
        public Builder routeKey(String value) { this.routeKey = value; return this; }
        public Builder expectedOwnerToken(String value) { this.expectedOwnerToken = value; return this; }
        public Builder expectedVersion(Long value) { this.expectedVersion = value; return this; }
        public Builder policyName(String value) { this.policyName = value; return this; }
        public Builder policy(IdempotencyPolicy value) { this.policy = value; return this; }

        public IdempotencyRecoveryRequest build() { return new IdempotencyRecoveryRequest(this); }
    }
}
