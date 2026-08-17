package com.xjtu.iron.idempotent.api.request;

import com.xjtu.iron.idempotent.api.policy.IdempotencyOptions;
import com.xjtu.iron.idempotent.api.policy.IdempotencyPolicy;

/**
 * 普通幂等执行请求。
 *
 * <p>V1.3 以后本对象只描述“这一次请求是谁”：</p>
 * <ul>
 *     <li>{@code key}：逻辑幂等主键；</li>
 *     <li>{@code requestHash}：同 key 请求内容指纹；</li>
 *     <li>{@code routeKey}：业务分片路由元数据；</li>
 *     <li>{@code policyName / policy}：选择这类请求使用的执行策略。</li>
 * </ul>
 *
 * <p>优先级：inline policy &gt; legacy options &gt; policyName &gt; default policy。</p>
 */
public final class IdempotencyRequest {

    private final String key;
    private final String requestHash;
    private final String routeKey;
    private final String policyName;
    private final IdempotencyPolicy policy;

    /**
     * V1.2 兼容字段。
     *
     * @deprecated 请使用 policyName 或 policy。
     */
    @Deprecated
    private final IdempotencyOptions options;

    private IdempotencyRequest(Builder builder) {
        this.key = builder.key;
        this.requestHash = builder.requestHash;
        this.routeKey = builder.routeKey;
        this.policyName = builder.policyName;
        this.policy = builder.policy;
        this.options = builder.options;
    }

    public static Builder builder() { return new Builder(); }

    public static IdempotencyRequest of(String key) {
        return builder().key(key).build();
    }

    public static IdempotencyRequest of(String key, String policyName) {
        return builder().key(key).policyName(policyName).build();
    }

    /**
     * @deprecated V1.3 请使用 {@link #of(String, String)} 或 inline policy。
     */
    @Deprecated
    public static IdempotencyRequest of(String key, IdempotencyOptions options) {
        return builder().key(key).options(options).build();
    }

    public String getKey() { return key; }
    public String getRequestHash() { return requestHash; }
    public String getRouteKey() { return routeKey; }
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
        private String policyName;
        private IdempotencyPolicy policy;
        private IdempotencyOptions options;

        public Builder key(String value) { this.key = value; return this; }
        public Builder requestHash(String value) { this.requestHash = value; return this; }
        public Builder routeKey(String value) { this.routeKey = value; return this; }
        public Builder policyName(String value) { this.policyName = value; return this; }
        public Builder policy(IdempotencyPolicy value) { this.policy = value; return this; }

        /**
         * @deprecated V1.3 请使用 policy/policyName。
         */
        @Deprecated
        public Builder options(IdempotencyOptions value) { this.options = value; return this; }

        public IdempotencyRequest build() { return new IdempotencyRequest(this); }
    }
}
