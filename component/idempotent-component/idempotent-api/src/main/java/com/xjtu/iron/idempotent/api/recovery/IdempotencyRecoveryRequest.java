package com.xjtu.iron.idempotent.api.recovery;

import com.xjtu.iron.idempotent.api.execution.IdempotencyExecutor;
import com.xjtu.iron.idempotent.api.policy.IdempotencyOptions;
import com.xjtu.iron.idempotent.api.policy.IdempotencyPolicy;

/**
 * 外部 Reliable Task 调用 {@link IdempotencyExecutor#recover} 时使用的恢复请求。
 *
 * <p>{@code expectedOwnerToken / expectedVersion} 用于避免过时扫描任务误接管。</p>
 */
public final class IdempotencyRecoveryRequest {

    private final String key;
    private final String requestHash;
    private final String routeKey;
    private final String expectedOwnerToken;
    private final Long expectedVersion;
    private final String policyName;
    private final IdempotencyPolicy policy;

    /**
     * @deprecated V1.3 请使用 policyName/policy。
     */
    @Deprecated
    private final IdempotencyOptions options;

    private IdempotencyRecoveryRequest(Builder builder) {
        this.key = builder.key;
        this.requestHash = builder.requestHash;
        this.routeKey = builder.routeKey;
        this.expectedOwnerToken = builder.expectedOwnerToken;
        this.expectedVersion = builder.expectedVersion;
        this.policyName = builder.policyName;
        this.policy = builder.policy;
        this.options = builder.options;
    }

    public static Builder builder() { return new Builder(); }

    public String getKey() { return key; }
    public String getRequestHash() { return requestHash; }
    public String getRouteKey() { return routeKey; }
    public String getExpectedOwnerToken() { return expectedOwnerToken; }
    public Long getExpectedVersion() { return expectedVersion; }
    public String getPolicyName() { return policyName; }
    public IdempotencyPolicy getPolicy() { return policy; }

    /**
     * @deprecated 仅用于 V1.2 兼容。
     */
    @Deprecated
    public IdempotencyOptions getOptions() { return options; }

    public static final class Builder {
        private String key;
        private String requestHash;
        private String routeKey;
        private String expectedOwnerToken;
        private Long expectedVersion;
        private String policyName;
        private IdempotencyPolicy policy;
        private IdempotencyOptions options;

        public Builder key(String value) { this.key = value; return this; }
        public Builder requestHash(String value) { this.requestHash = value; return this; }
        public Builder routeKey(String value) { this.routeKey = value; return this; }
        public Builder expectedOwnerToken(String value) { this.expectedOwnerToken = value; return this; }
        public Builder expectedVersion(Long value) { this.expectedVersion = value; return this; }
        public Builder policyName(String value) { this.policyName = value; return this; }
        public Builder policy(IdempotencyPolicy value) { this.policy = value; return this; }

        /**
         * @deprecated V1.3 请使用 policy/policyName。
         */
        @Deprecated
        public Builder options(IdempotencyOptions value) { this.options = value; return this; }

        public IdempotencyRecoveryRequest build() { return new IdempotencyRecoveryRequest(this); }
    }
}
