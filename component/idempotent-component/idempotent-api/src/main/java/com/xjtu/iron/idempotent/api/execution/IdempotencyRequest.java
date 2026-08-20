package com.xjtu.iron.idempotent.api.execution;

import com.xjtu.iron.idempotent.api.policy.IdempotencyPolicy;

/**
 * 普通幂等执行请求，只描述“这一次逻辑请求是谁”。
 *
 * <ul>
 *     <li>{@code key}：逻辑请求身份证；同一次 HTTP 重试/MQ 重投必须保持相同；</li>
 *     <li>{@code requestHash}：业务内容指纹，防止同 key 被不同参数错误复用；</li>
 *     <li>{@code routeKey}：分库分表/恢复任务路由元数据，不替代 idempotency key；</li>
 *     <li>{@code policyName / policy}：选择“这一类业务应该怎么做幂等”。</li>
 * </ul>
 *
 * <p>策略解析优先级：inline policy &gt; policyName &gt; default policy。</p>
 */
public final class IdempotencyRequest {

    private final String key;
    private final String requestHash;
    private final String routeKey;
    private final String policyName;
    private final IdempotencyPolicy policy;

    private IdempotencyRequest(Builder builder) {
        this.key = builder.key;
        this.requestHash = builder.requestHash;
        this.routeKey = builder.routeKey;
        this.policyName = builder.policyName;
        this.policy = builder.policy;
    }

    public static Builder builder() { return new Builder(); }
    public static IdempotencyRequest of(String key) { return builder().key(key).build(); }
    public static IdempotencyRequest of(String key, String policyName) { return builder().key(key).policyName(policyName).build(); }

    public String getKey() { return key; }
    public String getRequestHash() { return requestHash; }
    public String getRouteKey() { return routeKey; }
    public String getPolicyName() { return policyName; }
    public IdempotencyPolicy getPolicy() { return policy; }

    public static final class Builder {
        private String key;
        private String requestHash;
        private String routeKey;
        private String policyName;
        private IdempotencyPolicy policy;

        public Builder key(String value) { this.key = value; return this; }
        public Builder requestHash(String value) { this.requestHash = value; return this; }
        public Builder routeKey(String value) { this.routeKey = value; return this; }
        public Builder policyName(String value) { this.policyName = value; return this; }
        public Builder policy(IdempotencyPolicy value) { this.policy = value; return this; }

        public IdempotencyRequest build() { return new IdempotencyRequest(this); }
    }
}
